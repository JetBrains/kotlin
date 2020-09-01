/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter

import org.jetbrains.kotlin.ir.interpreter.builtins.compileTimeAnnotation
import org.jetbrains.kotlin.ir.interpreter.builtins.contractsDslAnnotation
import org.jetbrains.kotlin.ir.interpreter.builtins.evaluateIntrinsicAnnotation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.types.isString
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.FqName

enum class EvaluationMode {
    FULL, ONLY_BUILTINS
}

class IrCompileTimeChecker(
    containingDeclaration: IrElement? = null, private val mode: EvaluationMode = EvaluationMode.FULL
) : IrElementVisitor<Boolean, Nothing?> {
    private val visitedStack = mutableListOf<IrElement>().apply { if (containingDeclaration != null) add(containingDeclaration) }

    private val compileTimeTypeAliases = setOf(
        "java.lang.StringBuilder", "java.lang.IllegalArgumentException", "java.util.NoSuchElementException"
    )

    private fun IrDeclaration.isContract() = isMarkedWith(contractsDslAnnotation)
    private fun IrDeclaration.isMarkedAsEvaluateIntrinsic() = isMarkedWith(evaluateIntrinsicAnnotation)
    private fun IrDeclaration.isMarkedAsCompileTime(expression: IrCall? = null): Boolean {
        if (mode == EvaluationMode.FULL) {
            return isMarkedWith(compileTimeAnnotation) ||
                    (this is IrSimpleFunction && this.isFakeOverride && this.overriddenSymbols.any { it.owner.isMarkedAsCompileTime() }) ||
                    this.parentClassOrNull?.fqNameWhenAvailable?.asString() in compileTimeTypeAliases
        }

        val parent = this.parentClassOrNull
        val parentType = parent?.defaultType
        return when {
            parentType?.isPrimitiveType() == true -> (this as IrFunction).name.asString() !in setOf("inc", "dec", "rangeTo", "hashCode")
            parentType?.isString() == true -> (this as IrDeclarationWithName).name.asString() !in setOf("subSequence", "hashCode")
            parentType?.isAny() == true -> (this as IrFunction).name.asString() == "toString" && expression?.dispatchReceiver !is IrGetObjectValue
            parent?.isObject == true -> parent.parentClassOrNull?.defaultType?.let { it.isPrimitiveType() || it.isUnsigned() } == true
            else -> false
        }
    }

    private fun IrDeclaration.isMarkedWith(annotation: FqName): Boolean {
        if (this is IrClass && this.isCompanion) return false
        if (this.hasAnnotation(annotation)) return true
        return (this.parent as? IrClass)?.isMarkedWith(annotation) ?: false
    }

    private fun IrProperty?.isCompileTime(): Boolean {
        if (this == null) return false
        if (this.isConst) return true
        if (this.isMarkedAsCompileTime()) return true

        val backingField = this.backingField
        val backingFieldExpression = backingField?.initializer?.expression as? IrGetValue
        return backingFieldExpression?.origin == IrStatementOrigin.INITIALIZE_PROPERTY_FROM_PARAMETER
    }

    private fun IrElement.asVisited(block: () -> Boolean): Boolean {
        visitedStack += this
        val result = block()
        visitedStack.removeAt(visitedStack.lastIndex)
        return result
    }

    override fun visitElement(element: IrElement, data: Nothing?) = false

    private fun visitStatements(statements: List<IrStatement>, data: Nothing?): Boolean {
        if (mode == EvaluationMode.ONLY_BUILTINS) return false
        return statements.all { it.accept(this, data) }
    }

    private fun visitConstructor(expression: IrFunctionAccessExpression): Boolean {
        return when {
            expression.symbol.owner.isMarkedAsEvaluateIntrinsic() -> true
            !visitValueParameters(expression, null) -> false
            else -> expression.symbol.owner.isMarkedAsCompileTime()
        }
    }

    override fun visitCall(expression: IrCall, data: Nothing?): Boolean {
        if (expression.symbol.owner.isContract()) return false

        val property = (expression.symbol.owner as? IrSimpleFunction)?.correspondingPropertySymbol?.owner
        if (expression.symbol.owner.isMarkedAsCompileTime(expression) || property.isCompileTime()) {
            val dispatchReceiverComputable = expression.dispatchReceiver?.accept(this, null) ?: true
            val extensionReceiverComputable = expression.extensionReceiver?.accept(this, null) ?: true
            if (!visitValueParameters(expression, null)) return false
            val bodyComputable = if (expression.symbol.owner.isLocal) expression.symbol.owner.body?.accept(this, null) ?: true else true

            return dispatchReceiverComputable && extensionReceiverComputable && bodyComputable
        }

        return false
    }

    override fun visitVariable(declaration: IrVariable, data: Nothing?): Boolean {
        return declaration.initializer?.accept(this, data) ?: true
    }

    private fun visitValueParameters(expression: IrFunctionAccessExpression, data: Nothing?): Boolean {
        return (0 until expression.valueArgumentsCount)
            .map { expression.getValueArgument(it) }
            .none { it?.accept(this, data) == false }
    }

    override fun visitBody(body: IrBody, data: Nothing?): Boolean {
        return visitStatements(body.statements, data)
    }

    override fun visitBlock(expression: IrBlock, data: Nothing?): Boolean {
        return visitStatements(expression.statements, data)
    }

    override fun visitSyntheticBody(body: IrSyntheticBody, data: Nothing?): Boolean {
        return body.kind == IrSyntheticBodyKind.ENUM_VALUES || body.kind == IrSyntheticBodyKind.ENUM_VALUEOF
    }

    override fun <T> visitConst(expression: IrConst<T>, data: Nothing?): Boolean = true

    override fun visitVararg(expression: IrVararg, data: Nothing?): Boolean {
        return expression.elements.any { it.accept(this, data) }
    }

    override fun visitSpreadElement(spread: IrSpreadElement, data: Nothing?): Boolean {
        return spread.expression.accept(this, data)
    }

    override fun visitComposite(expression: IrComposite, data: Nothing?): Boolean {
        if (expression.origin == IrStatementOrigin.DESTRUCTURING_DECLARATION) {
            return visitStatements(expression.statements, data)
        }
        return false
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation, data: Nothing?): Boolean {
        return expression.arguments.all { it.accept(this, data) }
    }

    override fun visitGetObjectValue(expression: IrGetObjectValue, data: Nothing?): Boolean {
        // to get object value we need nothing but it will contain only fields with compile time annotation
        return true
    }

    override fun visitGetEnumValue(expression: IrGetEnumValue, data: Nothing?): Boolean {
        return expression.symbol.owner.initializerExpression?.accept(this, data) == true
    }

    override fun visitGetValue(expression: IrGetValue, data: Nothing?): Boolean {
        val parent = expression.symbol.owner.parent as IrSymbolOwner
        val isObject = (parent as? IrClass)?.isObject == true //used to evaluate constants inside object
        return visitedStack.contains(parent) || isObject
    }

    override fun visitSetValue(expression: IrSetValue, data: Nothing?): Boolean {
        return expression.value.accept(this, data)
    }

    override fun visitGetField(expression: IrGetField, data: Nothing?): Boolean {
        val owner = expression.symbol.owner
        val parent = owner.parent as IrSymbolOwner
        val isJavaPrimitiveStatic = owner.origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB && owner.isStatic &&
                owner.parentAsClass.fqNameWhenAvailable.isJavaPrimitive()
        return visitedStack.contains(parent) || isJavaPrimitiveStatic
    }

    // TODO find similar method in utils
    private fun FqName?.isJavaPrimitive(): Boolean {
        this ?: return false
        return this.toString() in setOf(
            "java.lang.Byte", "java.lang.Short", "java.lang.Integer", "java.lang.Long",
            "java.lang.Float", "java.lang.Double", "java.lang.Boolean", "java.lang.Character"
        )
    }

    override fun visitSetField(expression: IrSetField, data: Nothing?): Boolean {
        //todo check receiver?
        val parent = expression.symbol.owner.parent as IrSymbolOwner
        return visitedStack.contains(parent) && expression.value.accept(this, data)
    }

    override fun visitConstructorCall(expression: IrConstructorCall, data: Nothing?): Boolean {
        return visitConstructor(expression)
    }

    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall, data: Nothing?): Boolean {
        return visitConstructor(expression)
    }

    override fun visitFunctionReference(expression: IrFunctionReference, data: Nothing?): Boolean {
        return expression.asVisited {
            expression.symbol.owner.isMarkedAsCompileTime() && expression.symbol.owner.body?.accept(this, data) == true
        }
    }

    override fun visitFunctionExpression(expression: IrFunctionExpression, data: Nothing?): Boolean {
        val isLambda = expression.origin == IrStatementOrigin.LAMBDA || expression.origin == IrStatementOrigin.ANONYMOUS_FUNCTION
        val isCompileTime = expression.function.isMarkedAsCompileTime()
        return expression.function.asVisited {
            if (isLambda || isCompileTime) expression.function.body?.accept(this, data) == true else false
        }
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: Nothing?): Boolean {
        return when (expression.operator) {
            IrTypeOperator.INSTANCEOF, IrTypeOperator.NOT_INSTANCEOF,
            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT,
            IrTypeOperator.CAST, IrTypeOperator.IMPLICIT_CAST, IrTypeOperator.SAFE_CAST,
            IrTypeOperator.IMPLICIT_NOTNULL -> {
                val operand = expression.typeOperand.classifierOrNull?.owner
                if (operand is IrTypeParameter && !visitedStack.contains(operand.parent)) return false
                expression.argument.accept(this, data)
            }
            IrTypeOperator.IMPLICIT_DYNAMIC_CAST -> false
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
        if (mode == EvaluationMode.ONLY_BUILTINS) return false
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
}