/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.checker

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.interpreter.IrInterpreterConfiguration
import org.jetbrains.kotlin.ir.interpreter.accessesTopLevelOrObjectField
import org.jetbrains.kotlin.ir.interpreter.fqName
import org.jetbrains.kotlin.ir.interpreter.isAccessToNotNullableObject
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

class IrCompileTimeChecker(
    containingDeclaration: IrElement? = null,
    private val mode: EvaluationMode = EvaluationMode.WITH_ANNOTATIONS,
    private val interpreterConfiguration: IrInterpreterConfiguration,
) : IrElementVisitor<Boolean, Nothing?> {
    private var contextExpression: IrCall? = null
    private val visitedStack = mutableListOf<IrElement>().apply { if (containingDeclaration != null) add(containingDeclaration) }

    private fun IrElement.asVisited(block: () -> Boolean): Boolean {
        visitedStack += this
        val result = block()
        visitedStack.removeAt(visitedStack.lastIndex)
        return result
    }

    private fun <R> IrCall.saveContext(block: () -> R): R {
        val oldContext = contextExpression
        contextExpression = this
        return block().apply { contextExpression = oldContext }
    }

    override fun visitElement(element: IrElement, data: Nothing?) = false

    private fun IrDeclarationParent.getInnerDeclarations(): List<IrStatement> {
        return (this as? IrDeclarationContainer)?.declarations ?: (this as? IrStatementContainer)?.statements ?: emptyList()
    }

    private fun visitStatements(statements: List<IrStatement>): Boolean {
        when (mode) {
            EvaluationMode.ONLY_BUILTINS, EvaluationMode.ONLY_INTRINSIC_CONST -> {
                val statement = statements.singleOrNull() ?: return false
                return statement.accept(this, null)
            }
            else -> return statements.all { it.accept(this, null) }
        }
    }

    private fun visitConstructor(expression: IrFunctionAccessExpression): Boolean {
        return when {
            !visitValueArguments(expression, null) || !mode.canEvaluateFunction(expression.symbol.owner, contextExpression) -> false
            else -> expression.symbol.owner.visitBodyIfNeeded()
        }
    }

    private fun IrFunction.visitBodyIfNeeded(): Boolean {
        return this.asVisited { !mode.mustCheckBodyOf(this) || (this.body?.accept(this@IrCompileTimeChecker, null) ?: true) }
    }

    override fun visitCall(expression: IrCall, data: Nothing?): Boolean {
        val owner = expression.symbol.owner
        if (!mode.canEvaluateFunction(owner, expression)) return false

        // We disable `toFloat` folding on K/JS till `toFloat` is fixed (KT-35422)
        // This check must be placed here instead of CallInterceptor because we still
        // want to evaluate (1) `const val` expressions and (2) values in annotations.
        if (owner.name.asString() == "toFloat" && interpreterConfiguration.treatFloatInSpecialWay) {
            return super.visitCall(expression, data)
        }

        return expression.saveContext {
            val dispatchReceiverComputable = expression.dispatchReceiver?.accept(this, null) ?: true
            val extensionReceiverComputable = expression.extensionReceiver?.accept(this, null) ?: true
            if (!visitValueArguments(expression, null)) return@saveContext false
            val bodyComputable = owner.visitBodyIfNeeded()
            return@saveContext dispatchReceiverComputable && extensionReceiverComputable && bodyComputable
        }
    }

    override fun visitVariable(declaration: IrVariable, data: Nothing?): Boolean {
        return declaration.initializer?.accept(this, data) ?: true
    }

    private fun visitValueArguments(expression: IrFunctionAccessExpression, data: Nothing?): Boolean {
        return (0 until expression.valueArgumentsCount)
            .map { expression.getValueArgument(it) }
            .none { it?.accept(this, data) == false }
    }

    override fun visitBody(body: IrBody, data: Nothing?): Boolean {
        return visitStatements(body.statements)
    }

    // We need this separate method to explicitly indicate that IrExpressionBody can be interpreted in any evaluation mode
    override fun visitExpressionBody(body: IrExpressionBody, data: Nothing?): Boolean {
        return body.expression.accept(this, data)
    }

    override fun visitBlock(expression: IrBlock, data: Nothing?): Boolean {
        if (mode == EvaluationMode.ONLY_INTRINSIC_CONST && expression.origin == IrStatementOrigin.WHEN) {
            return expression.statements.all { it.accept(this, null) }
        }

        // `IrReturnableBlock` will be created from IrCall after inline. We should do basically the same check as for IrCall.
        if (expression is IrReturnableBlock) {
            // TODO after JVM inline MR 8122 will be pushed check original IrCall.
            TODO("Interpretation of `IrReturnableBlock` is not implemented")
        }

        return visitStatements(expression.statements)
    }

    override fun visitSyntheticBody(body: IrSyntheticBody, data: Nothing?): Boolean {
        return body.kind == IrSyntheticBodyKind.ENUM_VALUES || body.kind == IrSyntheticBodyKind.ENUM_VALUEOF
    }

    override fun visitConst(expression: IrConst<*>, data: Nothing?): Boolean {
        if (expression.type.getUnsignedType() != null) {
            val constructor = expression.type.classOrNull?.owner?.constructors?.singleOrNull() ?: return false
            return mode.canEvaluateFunction(constructor)
        }
        return true
    }

    override fun visitVararg(expression: IrVararg, data: Nothing?): Boolean {
        return expression.elements.any { it.accept(this, data) }
    }

    override fun visitSpreadElement(spread: IrSpreadElement, data: Nothing?): Boolean {
        return spread.expression.accept(this, data)
    }

    override fun visitComposite(expression: IrComposite, data: Nothing?): Boolean {
        if (expression.origin == IrStatementOrigin.DESTRUCTURING_DECLARATION || expression.origin == null) {
            return visitStatements(expression.statements)
        }
        return false
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation, data: Nothing?): Boolean {
        return expression.arguments.all { arg ->
            when (arg) {
                is IrGetObjectValue -> {
                    val toString = arg.symbol.owner.declarations
                        .filterIsInstance<IrSimpleFunction>()
                        .single { it.name.asString() == "toString" && it.valueParameters.isEmpty() && it.extensionReceiverParameter == null }

                    mode.canEvaluateFunction(toString, null) && toString.visitBodyIfNeeded()
                }

                else -> arg.accept(this, data)
            }
        }
    }

    override fun visitGetObjectValue(expression: IrGetObjectValue, data: Nothing?): Boolean {
        // to get object value we need nothing, but it will contain only fields with compile time annotation
        return true
    }

    override fun visitGetEnumValue(expression: IrGetEnumValue, data: Nothing?): Boolean {
        if (!mode.canEvaluateEnumValue(expression, contextExpression)) return false
        // we want to avoid recursion in cases like "enum class E(val srt: String) { OK(OK.name) }"
        if (visitedStack.contains(expression)) return true
        return expression.asVisited {
            expression.symbol.owner.initializerExpression?.accept(this, data) == true
        }
    }

    override fun visitGetValue(expression: IrGetValue, data: Nothing?): Boolean {
        return visitedStack.contains(expression.symbol.owner.parent) || expression.isAccessToNotNullableObject()
    }

    override fun visitSetValue(expression: IrSetValue, data: Nothing?): Boolean {
        return expression.value.accept(this, data)
    }

    override fun visitGetField(expression: IrGetField, data: Nothing?): Boolean {
        val owner = expression.symbol.owner
        val property = owner.correspondingPropertySymbol?.owner
        val fqName = owner.fqName
        fun isJavaStaticWithPrimitiveOrString(): Boolean {
            return owner.origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB && owner.isStatic &&
                    (owner.type.isPrimitiveType() || owner.type.isStringClassType())
        }
        return when {
            // TODO fix later; used it here because java boolean resolves very strange,
            //  its type is flexible (so its not primitive) and there is no initializer at backing field
            fqName == "java.lang.Boolean.FALSE" || fqName == "java.lang.Boolean.TRUE" -> true
            isJavaStaticWithPrimitiveOrString() -> owner.initializer?.accept(this, data) == true
            expression.receiver == null -> property?.isConst == true && owner.initializer?.accept(this, null) == true
            owner.origin == IrDeclarationOrigin.PROPERTY_BACKING_FIELD && property?.isConst == true -> {
                val receiverComputable = expression.receiver?.accept(this, null) ?: true
                val initializerComputable = owner.initializer?.accept(this, null) ?: false
                receiverComputable && initializerComputable
            }
            else -> {
                val declarations = owner.parent.getInnerDeclarations()
                val getter = declarations.filterIsInstance<IrProperty>().singleOrNull { it == property }?.getter ?: return false
                visitedStack.contains(getter)
            }
        }
    }

    override fun visitSetField(expression: IrSetField, data: Nothing?): Boolean {
        if (expression.accessesTopLevelOrObjectField()) return false
        //todo check receiver?
        val property = expression.symbol.owner.correspondingPropertySymbol?.owner
        val declarations = expression.symbol.owner.parent.getInnerDeclarations()
        val setter = declarations.filterIsInstance<IrProperty>().single { it == property }.setter ?: return false
        return visitedStack.contains(setter) && expression.value.accept(this, data)
    }

    override fun visitConstructorCall(expression: IrConstructorCall, data: Nothing?): Boolean {
        return visitConstructor(expression)
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: Nothing?): Boolean {
        if (expression.symbol.owner.returnType.isAny()) return true
        return visitConstructor(expression)
    }

    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall, data: Nothing?): Boolean {
        return visitConstructor(expression)
    }

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall, data: Nothing?): Boolean {
        val irClass = expression.classSymbol.owner
        val classProperties = irClass.declarations.filterIsInstance<IrProperty>()
        val anonymousInitializer = irClass.declarations.filterIsInstance<IrAnonymousInitializer>().filter { !it.isStatic }


        return anonymousInitializer.all { init -> init.body.accept(this, data) } && classProperties.all {
            val propertyInitializer = it.backingField?.initializer?.expression
            if ((propertyInitializer as? IrGetValue)?.origin == IrStatementOrigin.INITIALIZE_PROPERTY_FROM_PARAMETER) return@all true
            return@all (propertyInitializer?.accept(this, data) != false)
        }
    }

    override fun visitFunctionReference(expression: IrFunctionReference, data: Nothing?): Boolean {
        if (!mode.canEvaluateReference(expression, contextExpression)) return false

        val owner = expression.symbol.owner
        val dispatchReceiverComputable = expression.dispatchReceiver?.accept(this, null) ?: true
        val extensionReceiverComputable = expression.extensionReceiver?.accept(this, null) ?: true

        if (!mode.canEvaluateFunction(owner, contextExpression)) return false

        val bodyComputable = owner.visitBodyIfNeeded()
        return dispatchReceiverComputable && extensionReceiverComputable && bodyComputable
    }

    override fun visitFunctionExpression(expression: IrFunctionExpression, data: Nothing?): Boolean {
        if (mode == EvaluationMode.ONLY_BUILTINS || mode == EvaluationMode.ONLY_INTRINSIC_CONST) return false
        val isLambda = expression.origin == IrStatementOrigin.LAMBDA || expression.origin == IrStatementOrigin.ANONYMOUS_FUNCTION
        val isCompileTime = mode.canEvaluateFunction(expression.function)
        return expression.function.asVisited {
            if (isLambda || isCompileTime) expression.function.body?.accept(this, data) == true else false
        }
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: Nothing?): Boolean {
        return when (expression.operator) {
            IrTypeOperator.INSTANCEOF, IrTypeOperator.NOT_INSTANCEOF,
            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT, IrTypeOperator.IMPLICIT_NOTNULL, IrTypeOperator.SAM_CONVERSION,
            IrTypeOperator.CAST, IrTypeOperator.IMPLICIT_CAST, IrTypeOperator.SAFE_CAST -> {
                val operand = expression.typeOperand.classifierOrNull?.owner
                if (operand is IrTypeParameter && !visitedStack.contains(operand.parent)) return false
                expression.argument.accept(this, data)
            }
            else -> false
        }
    }

    override fun visitWhen(expression: IrWhen, data: Nothing?): Boolean {
        return expression.branches.all { it.accept(this, data) }
    }

    override fun visitBranch(branch: IrBranch, data: Nothing?): Boolean {
        return branch.condition.accept(this, data) && branch.result.accept(this, data)
    }

    override fun visitWhileLoop(loop: IrWhileLoop, data: Nothing?): Boolean {
        return loop.asVisited {
            loop.condition.accept(this, data) && (loop.body?.accept(this, data) ?: true)
        }
    }

    override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: Nothing?): Boolean {
        return loop.asVisited {
            loop.condition.accept(this, data) && (loop.body?.accept(this, data) ?: true)
        }
    }

    override fun visitTry(aTry: IrTry, data: Nothing?): Boolean {
        if (mode == EvaluationMode.ONLY_BUILTINS || mode == EvaluationMode.ONLY_INTRINSIC_CONST) return false
        if (!aTry.tryResult.accept(this, data)) return false
        if (aTry.finallyExpression != null && aTry.finallyExpression?.accept(this, data) == false) return false
        return aTry.catches.all { it.result.accept(this, data) }
    }

    override fun visitBreak(jump: IrBreak, data: Nothing?): Boolean = visitedStack.contains(jump.loop)

    override fun visitContinue(jump: IrContinue, data: Nothing?): Boolean = visitedStack.contains(jump.loop)

    override fun visitReturn(expression: IrReturn, data: Nothing?): Boolean {
        if (!visitedStack.contains(expression.returnTargetSymbol.owner)) return false
        return expression.value.accept(this, data)
    }

    override fun visitThrow(expression: IrThrow, data: Nothing?): Boolean {
        return expression.value.accept(this, data)
    }

    override fun visitPropertyReference(expression: IrPropertyReference, data: Nothing?): Boolean {
        if (!mode.canEvaluateReference(expression, contextExpression)) return false

        val dispatchReceiverComputable = expression.dispatchReceiver?.accept(this, null) ?: true
        val extensionReceiverComputable = expression.extensionReceiver?.accept(this, null) ?: true

        val getterIsComputable = expression.getter?.let { mode.canEvaluateFunction(it.owner, contextExpression) } ?: true
        return dispatchReceiverComputable && extensionReceiverComputable && getterIsComputable
    }

    override fun visitClassReference(expression: IrClassReference, data: Nothing?): Boolean {
        return with(mode) {
            when (this) {
                EvaluationMode.FULL -> true
                EvaluationMode.WITH_ANNOTATIONS -> (expression.symbol.owner as IrClass).isMarkedAsCompileTime()
                EvaluationMode.ONLY_BUILTINS, EvaluationMode.ONLY_INTRINSIC_CONST -> false
            }
        }
    }
}