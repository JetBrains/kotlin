/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.checker

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.interpreter.*
import org.jetbrains.kotlin.ir.interpreter.accessesTopLevelOrObjectField
import org.jetbrains.kotlin.ir.interpreter.correspondingProperty
import org.jetbrains.kotlin.ir.interpreter.fqName
import org.jetbrains.kotlin.ir.interpreter.isAccessToNotNullableObject
import org.jetbrains.kotlin.ir.interpreter.preprocessor.IrInterpreterKCallableNamePreprocessor.Companion.isEnumName
import org.jetbrains.kotlin.ir.interpreter.preprocessor.IrInterpreterKCallableNamePreprocessor.Companion.isKCallableNameCall
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.types.isStringClassType
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.statements

class IrInterpreterCommonChecker : IrInterpreterChecker {
    private val visitedStack = mutableListOf<IrElement>()

    private inline fun IrElement.asVisited(crossinline block: () -> Boolean): Boolean {
        visitedStack += this
        val result = block()
        visitedStack.removeAt(visitedStack.lastIndex)
        return result
    }

    override fun visitElement(element: IrElement, data: IrInterpreterCheckerData) = false

    private fun IrDeclarationParent.getInnerDeclarations(): List<IrStatement> {
        return (this as? IrDeclarationContainer)?.declarations ?: (this as? IrStatementContainer)?.statements ?: emptyList()
    }

    private fun visitStatements(statements: List<IrStatement>, data: IrInterpreterCheckerData): Boolean {
        return statements.all { it.accept(this, data) }
    }

    private fun visitConstructor(expression: IrFunctionAccessExpression, data: IrInterpreterCheckerData): Boolean {
        val constructor = expression.symbol.owner

        if (!data.mode.canEvaluateFunction(constructor)) return false
        if (!visitValueArguments(expression, data)) return false
        return visitBodyIfNeeded(constructor, data) &&
                constructor.parentAsClass.declarations.filterIsInstance<IrAnonymousInitializer>().all { it.accept(this, data) }
    }

    private fun visitBodyIfNeeded(irFunction: IrFunction, data: IrInterpreterCheckerData): Boolean {
        if (!data.mode.mustCheckBodyOf(irFunction)) return true
        return irFunction.asVisited { irFunction.body?.accept(this@IrInterpreterCommonChecker, data) ?: true }
    }

    private fun IrCall.isGetterToConstVal(): Boolean {
        return correspondingProperty.isConst
    }

    override fun visitCall(expression: IrCall, data: IrInterpreterCheckerData): Boolean {
        val owner = expression.symbol.owner
        return when {
            expression.dispatchReceiver.isAccessToNotNullableObject() && expression.isGetterToConstVal() -> visitBodyIfNeeded(owner, data)
            !data.mode.canEvaluateExpression(expression) || !data.mode.canEvaluateFunction(owner) -> false
            expression.isKCallableNameCall(data.irBuiltIns) || expression.isEnumName() -> true
            else -> {
                val dispatchReceiverComputable = expression.dispatchReceiver?.accept(this, data) ?: true
                val extensionReceiverComputable = expression.extensionReceiver?.accept(this, data) ?: true
                dispatchReceiverComputable &&
                        extensionReceiverComputable &&
                        visitValueArguments(expression, data) &&
                        visitBodyIfNeeded(owner, data)
            }
        }
    }

    override fun visitVariable(declaration: IrVariable, data: IrInterpreterCheckerData): Boolean {
        return declaration.initializer?.accept(this, data) ?: true
    }

    private fun visitValueArguments(expression: IrFunctionAccessExpression, data: IrInterpreterCheckerData): Boolean {
        return (0 until expression.valueArgumentsCount)
            .map { expression.getValueArgument(it) }
            .none { it?.accept(this, data) == false }
    }

    override fun visitBody(body: IrBody, data: IrInterpreterCheckerData): Boolean {
        return visitStatements(body.statements, data)
    }

    // We need this separate method to explicitly indicate that IrExpressionBody can be interpreted in any evaluation mode
    override fun visitExpressionBody(body: IrExpressionBody, data: IrInterpreterCheckerData): Boolean {
        return body.expression.accept(this, data)
    }

    override fun visitBlock(expression: IrBlock, data: IrInterpreterCheckerData): Boolean {
        if (!data.mode.canEvaluateBlock(expression)) return false

        // `IrReturnableBlock` will be created from IrCall after inline. We should do basically the same check as for IrCall.
        if (expression is IrReturnableBlock) {
            val inlinedBlock = expression.statements.singleOrNull() as? IrInlinedFunctionBlock
            if (inlinedBlock != null) return inlinedBlock.inlineCall.accept(this, data)
        }

        return visitStatements(expression.statements, data)
    }

    override fun visitComposite(expression: IrComposite, data: IrInterpreterCheckerData): Boolean {
        if (!data.mode.canEvaluateComposite(expression)) return false

        return visitStatements(expression.statements, data)
    }

    override fun visitSyntheticBody(body: IrSyntheticBody, data: IrInterpreterCheckerData): Boolean {
        return body.kind == IrSyntheticBodyKind.ENUM_VALUES || body.kind == IrSyntheticBodyKind.ENUM_VALUEOF
    }

    override fun visitConst(expression: IrConst<*>, data: IrInterpreterCheckerData): Boolean {
        return true
    }

    override fun visitVararg(expression: IrVararg, data: IrInterpreterCheckerData): Boolean {
        return expression.elements.any { it.accept(this, data) }
    }

    override fun visitSpreadElement(spread: IrSpreadElement, data: IrInterpreterCheckerData): Boolean {
        return spread.expression.accept(this, data)
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation, data: IrInterpreterCheckerData): Boolean {
        return expression.arguments.all { arg ->
            when (arg) {
                is IrGetObjectValue -> {
                    val toString = arg.symbol.owner.declarations
                        .filterIsInstance<IrSimpleFunction>()
                        .single { it.name.asString() == "toString" && it.valueParameters.isEmpty() && it.extensionReceiverParameter == null }

                    data.mode.canEvaluateFunction(toString) && visitBodyIfNeeded(toString, data)
                }

                else -> arg.accept(this, data)
            }
        }
    }

    override fun visitGetObjectValue(expression: IrGetObjectValue, data: IrInterpreterCheckerData): Boolean {
        return data.mode.canEvaluateExpression(expression)
    }

    override fun visitGetEnumValue(expression: IrGetEnumValue, data: IrInterpreterCheckerData): Boolean {
        if (!data.mode.canEvaluateEnumValue(expression)) return false

        // we want to avoid recursion in cases like "enum class E(val srt: String) { OK(OK.name) }"
        if (visitedStack.contains(expression)) return true
        return expression.asVisited {
            expression.symbol.owner.initializerExpression?.accept(this, data) == true
        }
    }

    override fun visitGetValue(expression: IrGetValue, data: IrInterpreterCheckerData): Boolean {
        return visitedStack.contains(expression.symbol.owner.parent)
    }

    override fun visitSetValue(expression: IrSetValue, data: IrInterpreterCheckerData): Boolean {
        return expression.value.accept(this, data)
    }

    override fun visitGetField(expression: IrGetField, data: IrInterpreterCheckerData): Boolean {
        val owner = expression.symbol.owner
        val property = owner.property
        val fqName = owner.fqName
        fun isJavaStaticWithPrimitiveOrString(): Boolean {
            return owner.origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB && owner.isStatic && owner.isFinal &&
                    (owner.type.isPrimitiveType() || owner.type.isStringClassType())
        }

        // We allow recursion access, but it will fail during interpretation. This way it is easier to implement error reporting.
        if (visitedStack.contains(owner)) return true

        return owner.asVisited {
            when {
                // TODO fix later; used it here because java boolean resolves very strange,
                //  its type is flexible (so its not primitive) and there is no initializer at backing field
                fqName == "java.lang.Boolean.FALSE" || fqName == "java.lang.Boolean.TRUE" -> true
                isJavaStaticWithPrimitiveOrString() -> owner.initializer?.accept(this, data) == true
                expression.receiver == null -> property.isConst && owner.initializer?.accept(this, data) == true
                owner.origin == IrDeclarationOrigin.PROPERTY_BACKING_FIELD && property.isConst -> {
                    val receiverComputable = (expression.receiver?.accept(this, data) ?: true)
                            || expression.receiver.isAccessToNotNullableObject()
                    val initializerComputable = owner.initializer?.accept(this, data) ?: false
                    receiverComputable && initializerComputable
                }
                else -> {
                    val declarations = owner.parent.getInnerDeclarations()
                    val getter = declarations.filterIsInstance<IrProperty>().singleOrNull { it == property }?.getter ?: return@asVisited false
                    visitedStack.contains(getter)
                }
            }
        }
    }

    override fun visitSetField(expression: IrSetField, data: IrInterpreterCheckerData): Boolean {
        if (expression.accessesTopLevelOrObjectField()) return false
        //todo check receiver?
        val property = expression.symbol.owner.property
        val declarations = expression.symbol.owner.parent.getInnerDeclarations()
        val setter = declarations.filterIsInstance<IrProperty>().single { it == property }.setter ?: return false
        return visitedStack.contains(setter) && expression.value.accept(this, data)
    }

    override fun visitConstructorCall(expression: IrConstructorCall, data: IrInterpreterCheckerData): Boolean {
        return visitConstructor(expression, data)
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: IrInterpreterCheckerData): Boolean {
        if (expression.symbol.owner.returnType.isAny()) return true
        return visitConstructor(expression, data)
    }

    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall, data: IrInterpreterCheckerData): Boolean {
        return visitConstructor(expression, data)
    }

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall, data: IrInterpreterCheckerData): Boolean {
        val irClass = expression.classSymbol.owner
        val classProperties = irClass.declarations.filterIsInstance<IrProperty>()
        val anonymousInitializer = irClass.declarations.filterIsInstance<IrAnonymousInitializer>().filter { !it.isStatic }

        return anonymousInitializer.all { init -> init.body.accept(this, data) } && classProperties.all {
            val propertyInitializer = it.backingField?.initializer?.expression
            if ((propertyInitializer as? IrGetValue)?.origin == IrStatementOrigin.INITIALIZE_PROPERTY_FROM_PARAMETER) return@all true
            return@all (propertyInitializer?.accept(this, data) != false)
        }
    }

    override fun visitFunctionReference(expression: IrFunctionReference, data: IrInterpreterCheckerData): Boolean {
        if (!data.mode.canEvaluateCallableReference(expression)) return false

        val owner = expression.symbol.owner
        val dispatchReceiverComputable = expression.dispatchReceiver?.accept(this, data) ?: true
        val extensionReceiverComputable = expression.extensionReceiver?.accept(this, data) ?: true

        if (!data.mode.canEvaluateFunction(owner)) return false

        val bodyComputable = visitBodyIfNeeded(owner, data)
        return dispatchReceiverComputable && extensionReceiverComputable && bodyComputable
    }

    override fun visitFunctionExpression(expression: IrFunctionExpression, data: IrInterpreterCheckerData): Boolean {
        if (!data.mode.canEvaluateFunctionExpression(expression)) return false

        val body = expression.function.body ?: return false
        return expression.function.asVisited { body.accept(this, data) }
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: IrInterpreterCheckerData): Boolean {
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

    override fun visitWhen(expression: IrWhen, data: IrInterpreterCheckerData): Boolean {
        if (!data.mode.canEvaluateExpression(expression)) return false

        return expression.branches.all { it.accept(this, data) }
    }

    override fun visitBranch(branch: IrBranch, data: IrInterpreterCheckerData): Boolean {
        return branch.condition.accept(this, data) && branch.result.accept(this, data)
    }

    override fun visitWhileLoop(loop: IrWhileLoop, data: IrInterpreterCheckerData): Boolean {
        return loop.asVisited {
            loop.condition.accept(this, data) && (loop.body?.accept(this, data) ?: true)
        }
    }

    override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: IrInterpreterCheckerData): Boolean {
        return loop.asVisited {
            loop.condition.accept(this, data) && (loop.body?.accept(this, data) ?: true)
        }
    }

    override fun visitTry(aTry: IrTry, data: IrInterpreterCheckerData): Boolean {
        if (!data.mode.canEvaluateExpression(aTry)) return false

        if (!aTry.tryResult.accept(this, data)) return false
        if (aTry.finallyExpression != null && aTry.finallyExpression?.accept(this, data) == false) return false
        return aTry.catches.all { it.result.accept(this, data) }
    }

    override fun visitBreak(jump: IrBreak, data: IrInterpreterCheckerData): Boolean = visitedStack.contains(jump.loop)

    override fun visitContinue(jump: IrContinue, data: IrInterpreterCheckerData): Boolean = visitedStack.contains(jump.loop)

    override fun visitReturn(expression: IrReturn, data: IrInterpreterCheckerData): Boolean {
        if (!visitedStack.contains(expression.returnTargetSymbol.owner)) return false
        return expression.value.accept(this, data)
    }

    override fun visitThrow(expression: IrThrow, data: IrInterpreterCheckerData): Boolean {
        if (!data.mode.canEvaluateExpression(expression)) return false

        return expression.value.accept(this, data)
    }

    override fun visitPropertyReference(expression: IrPropertyReference, data: IrInterpreterCheckerData): Boolean {
        if (!data.mode.canEvaluateCallableReference(expression)) return false

        val dispatchReceiverComputable = expression.dispatchReceiver?.accept(this, data) ?: true
        val extensionReceiverComputable = expression.extensionReceiver?.accept(this, data) ?: true

        val getterIsComputable = expression.getter?.let { data.mode.canEvaluateFunction(it.owner) } ?: true
        return dispatchReceiverComputable && extensionReceiverComputable && getterIsComputable
    }

    override fun visitClassReference(expression: IrClassReference, data: IrInterpreterCheckerData): Boolean {
        return data.mode.canEvaluateClassReference(expression)
    }
}
