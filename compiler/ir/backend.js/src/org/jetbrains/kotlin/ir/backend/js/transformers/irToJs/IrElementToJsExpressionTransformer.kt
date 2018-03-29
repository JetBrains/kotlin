/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.backend.js.utils.JsGenerationContext
import org.jetbrains.kotlin.ir.backend.js.utils.Namer
import org.jetbrains.kotlin.ir.backend.js.utils.isSpecial
import org.jetbrains.kotlin.ir.backend.js.utils.name
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.js.backend.ast.*

class IrElementToJsExpressionTransformer : BaseIrElementToJsNodeTransformer<JsExpression, JsGenerationContext> {
    override fun visitExpressionBody(body: IrExpressionBody, context: JsGenerationContext): JsExpression {
        return body.expression.accept(this, context)
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
            is IrConstKind.Long,
            is IrConstKind.Char -> super.visitConst(expression, context)
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
        return JsNameRef(expression.symbol.name.asString(), expression.receiver?.accept(this, context))
    }

    override fun visitGetValue(expression: IrGetValue, context: JsGenerationContext): JsExpression {

        return if (expression.symbol.isSpecial) {
            context.getSpecialRefForName(expression.symbol.name)
        } else {
            JsNameRef(context.getNameForSymbol(expression.symbol))
        }

    }

    override fun visitSetField(expression: IrSetField, context: JsGenerationContext): JsExpression {
        val dest = JsNameRef(expression.symbol.name.asString(), expression.receiver?.accept(this, context))
        val source = expression.value.accept(this, context)
        return jsAssignment(dest, source)
    }

    override fun visitSetVariable(expression: IrSetVariable, context: JsGenerationContext): JsExpression {
        val ref = JsNameRef(expression.symbol.name.toJsName())
        val value = expression.value.accept(this, context)
        return JsBinaryOperation(JsBinaryOperator.ASG, ref, value)
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, context: JsGenerationContext): JsExpression {
        val classNameRef = expression.symbol.owner.descriptor.constructedClass.name.toJsName().makeRef()
        val callFuncRef = JsNameRef(Namer.CALL_FUNCTION, classNameRef)
        val fromPrimary = context.currentFunction is IrConstructor
        val thisRef = if (fromPrimary) JsThisRef() else JsNameRef("\$this")
        val arguments = translateCallArguments(expression, context)
        return JsInvocation(callFuncRef, listOf(thisRef) + arguments)
    }

    override fun visitCall(expression: IrCall, context: JsGenerationContext): JsExpression {
        // TODO rewrite more accurately, right now it just copy-pasted and adopted from old version
        // TODO support:
        // * ir intrinsics
        // * js be intrinsics
        // * js function
        // * getters and setters
        // * binary and unary operations

        if (expression.symbol == context.staticContext.backendContext.objectCreate.symbol) {
            // TODO: temporary workaround until there is no an intrinsic infrastructure
            assert(expression.typeArgumentsCount == 1 && expression.valueArgumentsCount == 0)
            val classToCreate = expression.getTypeArgument(0)!!
            val prototype = prototypeOf(classToCreate.constructor.declarationDescriptor!!.name.toJsName().makeRef())
            return JsInvocation(Namer.JS_OBJECT_CREATE_FUNCTION, prototype)
        }

        val symbol = expression.symbol

        val dispatchReceiver = expression.dispatchReceiver?.accept(this, context)
        val extensionReceiver = expression.extensionReceiver?.accept(this, context)


        val arguments = translateCallArguments(expression, context)

        return if (symbol is IrConstructorSymbol) {
            JsNew(JsNameRef((symbol.owner.parent as IrClass).name.asString()), arguments)
        } else {
            // TODO sanitize name
            val symbolName = context.getNameForSymbol(symbol)
            val ref = if (dispatchReceiver != null) JsNameRef(symbolName, dispatchReceiver) else JsNameRef(symbolName)
            JsInvocation(ref, extensionReceiver?.let { listOf(extensionReceiver) + arguments } ?: arguments)
        }
    }

    override fun visitWhen(expression: IrWhen, context: JsGenerationContext): JsExpression {
        // TODO check when w/o else branch and empty when
        return expression.toJsNode(this, context, ::JsConditional)!!
    }
}