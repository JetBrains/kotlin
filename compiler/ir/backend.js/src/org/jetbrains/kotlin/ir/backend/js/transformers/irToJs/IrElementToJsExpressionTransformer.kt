/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.backend.js.utils.JsGenerationContext
import org.jetbrains.kotlin.ir.backend.js.utils.Namer
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.isFunctionTypeOrSubtype
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.util.OperatorNameConventions

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class IrElementToJsExpressionTransformer : BaseIrElementToJsNodeTransformer<JsExpression, JsGenerationContext> {

    override fun visitVararg(expression: IrVararg, context: JsGenerationContext): JsExpression {
        // TODO: perform the dark magic below in the separated lowering
        if (expression.elements.size == 1) {
            val element = expression.elements[0]
            if (element is IrSpreadElement) {
                // special case, invoke slice()
                val expr = element.expression.accept(this, context)
                return JsInvocation(JsNameRef(Namer.SLICE_FUNCTION, expr))
            }
        }

        var arrayLiteralElements = mutableListOf<JsExpression>()
        val concatArguments = mutableListOf<JsExpression>()
        var qualifier: JsExpression? = null

        expression.elements.forEach {
            if (it is IrSpreadElement) {
                val expr = it.expression.accept(this, context)
                if (qualifier == null) {
                    if (arrayLiteralElements.isEmpty()) {
                        qualifier = JsNameRef(Namer.CONCAT_FUNCTION, expr)
                    } else {
                        val dispatch = JsArrayLiteral(arrayLiteralElements)
                        arrayLiteralElements = mutableListOf()
                        qualifier = JsNameRef(Namer.CONCAT_FUNCTION, dispatch)
                        concatArguments.add(expr)
                    }
                } else {
                    if (arrayLiteralElements.isNotEmpty()) {
                        concatArguments.add(JsArrayLiteral(arrayLiteralElements))
                        arrayLiteralElements = mutableListOf()
                    }
                    concatArguments.add(expr)
                }
            } else {
                arrayLiteralElements.add(it.accept(this, context))
            }
        }

        return qualifier?.let {
            if (arrayLiteralElements.isNotEmpty()) {
                concatArguments.add(JsArrayLiteral(arrayLiteralElements))
            }
            return JsInvocation(it, concatArguments)
        } ?: JsArrayLiteral(arrayLiteralElements)
    }

    override fun visitExpressionBody(body: IrExpressionBody, context: JsGenerationContext): JsExpression =
        body.expression.accept(this, context)

    override fun visitFunctionReference(expression: IrFunctionReference, context: JsGenerationContext): JsExpression {
        val irFunction = expression.symbol.owner
        return irFunction.accept(IrFunctionToJsTransformer(), context).apply { name = null }
    }

    override fun <T> visitConst(expression: IrConst<T>, context: JsGenerationContext): JsExpression {
        val kind = expression.kind
        return when (kind) {
            is IrConstKind.String -> JsStringLiteral(kind.valueOf(expression))
            is IrConstKind.Null -> JsNullLiteral()
            is IrConstKind.Boolean -> JsBooleanLiteral(kind.valueOf(expression))
            is IrConstKind.Byte -> JsIntLiteral(kind.valueOf(expression).toInt())
            is IrConstKind.Short -> JsIntLiteral(kind.valueOf(expression).toInt())
            is IrConstKind.Int -> JsIntLiteral(kind.valueOf(expression))
            is IrConstKind.Long -> throw IllegalStateException("Long const should have been lowered at this point")
            is IrConstKind.Char -> throw IllegalStateException("Char const should have been lowered at this point")
            is IrConstKind.Float -> JsDoubleLiteral(kind.valueOf(expression).toDouble())
            is IrConstKind.Double -> JsDoubleLiteral(kind.valueOf(expression))
        }
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation, context: JsGenerationContext): JsExpression {
        // TODO revisit
        return expression.arguments.fold<IrExpression, JsExpression>(JsStringLiteral("")) { jsExpr, irExpr ->
            JsBinaryOperation(
                JsBinaryOperator.ADD,
                jsExpr,
                irExpr.accept(this, context)
            )
        }
    }

    override fun visitGetField(expression: IrGetField, context: JsGenerationContext): JsExpression {
        val fieldName = context.getNameForSymbol(expression.symbol)
        return JsNameRef(fieldName, expression.receiver?.accept(this, context))
    }

    override fun visitGetValue(expression: IrGetValue, context: JsGenerationContext): JsExpression =
        context.getNameForSymbol(expression.symbol).makeRef()

    override fun visitGetObjectValue(expression: IrGetObjectValue, context: JsGenerationContext) = when (expression.symbol.owner.kind) {
        ClassKind.OBJECT -> {
            // TODO: return unit instance instead of null
            if (expression.type.isUnit()) JsNullLiteral()
            else {
                val className = context.getNameForSymbol(expression.symbol)
                val getInstanceName = className.ident + "_getInstance"
                JsInvocation(JsNameRef(getInstanceName))
            }
        }
        else -> TODO()
    }


    override fun visitSetField(expression: IrSetField, context: JsGenerationContext): JsExpression {
        val fieldName = context.getNameForSymbol(expression.symbol)
        val dest = JsNameRef(fieldName, expression.receiver?.accept(this, context))
        val source = expression.value.accept(this, context)
        return jsAssignment(dest, source)
    }

    override fun visitSetVariable(expression: IrSetVariable, context: JsGenerationContext): JsExpression {
        val ref = JsNameRef(context.getNameForSymbol(expression.symbol))
        val value = expression.value.accept(this, context)
        return JsBinaryOperation(JsBinaryOperator.ASG, ref, value)
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, context: JsGenerationContext): JsExpression {
        val classNameRef = context.getNameForSymbol(expression.symbol).makeRef()
        val callFuncRef = JsNameRef(Namer.CALL_FUNCTION, classNameRef)
        val fromPrimary = context.currentFunction is IrConstructor
        val thisRef =
            if (fromPrimary) JsThisRef() else context.getNameForSymbol(context.currentFunction!!.valueParameters.last().symbol).makeRef()
        val arguments = translateCallArguments(expression, context)
        return JsInvocation(callFuncRef, listOf(thisRef) + arguments)
    }

    override fun visitCall(expression: IrCall, context: JsGenerationContext): JsExpression {
        val symbol = expression.symbol

        context.staticContext.intrinsics[symbol]?.let {
            return it(expression, context)
        }

        val dispatchReceiver = expression.dispatchReceiver
        val jsDispatchReceiver = expression.dispatchReceiver?.accept(this, context)
        val jsExtensionReceiver = expression.extensionReceiver?.accept(this, context)
        val arguments = translateCallArguments(expression, context)

        val isSuspend = (symbol.owner as? IrSimpleFunction)?.isSuspend ?: false

        if (dispatchReceiver != null && symbol.owner.name == OperatorNameConventions.INVOKE && !isSuspend && dispatchReceiver.type.isFunctionTypeOrSubtype()
        ) {
            return JsInvocation(jsDispatchReceiver!!, arguments)
        }

        expression.superQualifierSymbol?.let {
            val qualifierName = context.getNameForSymbol(it).makeRef()
            val targetName = context.getNameForSymbol(symbol)
            val qPrototype = JsNameRef(targetName, prototypeOf(qualifierName))
            val callRef = JsNameRef(Namer.CALL_FUNCTION, qPrototype)
            return JsInvocation(callRef, jsDispatchReceiver?.let { listOf(it) + arguments } ?: arguments)
        }

        return if (symbol is IrConstructorSymbol) {
            JsNew(context.getNameForSymbol(symbol).makeRef(), arguments)
        } else {
            val symbolName = context.getNameForSymbol(symbol)
            val ref = if (jsDispatchReceiver != null) JsNameRef(symbolName, jsDispatchReceiver) else JsNameRef(symbolName)
            JsInvocation(ref, jsExtensionReceiver?.let { listOf(jsExtensionReceiver) + arguments } ?: arguments)
        }
    }

    override fun visitWhen(expression: IrWhen, context: JsGenerationContext): JsExpression {
        // TODO check when w/o else branch and empty when
        return expression.toJsNode(this, context, ::JsConditional)!!
    }
}