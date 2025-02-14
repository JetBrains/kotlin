/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.getVoid
import org.jetbrains.kotlin.ir.backend.js.utils.realOverrideTarget
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.types.isNullableString
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.filterIsInstanceAnd

/**
 * Links [kotlin.Throwable] and JavaScript `Error` together to provide proper interop between language and platform exceptions.
 */
class ThrowableLowering(val context: JsIrBackendContext) : BodyLoweringPass {
    private val throwableClass = context.throwableClass
    private val throwableConstructors = context.throwableConstructors
    private val newThrowableFunction = context.newThrowableSymbol
    private val extendThrowableFunction = context.extendThrowableSymbol
    private val setupCauseParameter = context.setupCauseParameterSymbol
    private val setPropertiesToThrowableInstanceSymbol = context.setPropertiesToThrowableInstanceSymbol

    private fun undefinedValue(): IrExpression = context.getVoid()

    data class ThrowableArguments(
        val message: IrExpression?,
        val cause: IrExpression?
    )

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildren(Transformer(), container as? IrConstructor)
    }

    private fun IrFunctionAccessExpression.extractThrowableArguments(): ThrowableArguments =
        when (arguments.size) {
            0 -> ThrowableArguments(null, null)
            2 -> ThrowableArguments(message = arguments[0], cause = arguments[1])
            else -> {
                val arg = arguments[0]!!
                val parameter = symbol.owner.parameters[0]
                when {
                    parameter.type.isNullableString() -> ThrowableArguments(message = arg, cause = null)
                    else -> {
                        assert(parameter.type.makeNotNull().isThrowable())
                        ThrowableArguments(message = null, cause = arg)
                    }
                }
            }
        }

    inner class Transformer : IrTransformer<IrConstructor?>() {
        private val anyConstructor = context.irBuiltIns.anyClass.constructors.first()

        override fun visitConstructor(declaration: IrConstructor, data: IrConstructor?) =
            super.visitConstructor(declaration, declaration)

        override fun visitConstructorCall(expression: IrConstructorCall, data: IrConstructor?): IrExpression {
            expression.transformChildren(this, data)
            if (expression.symbol !in throwableConstructors) return expression

            val (messageArg, causeArg) = expression.extractThrowableArguments()

            return expression.run {
                IrCallImpl(
                    startOffset, endOffset, type, newThrowableFunction,
                    typeArgumentsCount = 0
                ).also {
                    it.arguments[0] = messageArg ?: undefinedValue()
                    it.arguments[1] = causeArg ?: undefinedValue()
                }
            }
        }

        override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: IrConstructor?): IrExpression {
            expression.transformChildren(this, data)
            if (expression.symbol !in throwableConstructors) return expression

            val currentConstructor = data ?: compilationException("Delegation call outside of constructor", expression)
            val klass = currentConstructor.constructedClass

            val (messageArg, causeArg) = expression.extractThrowableArguments()
            val thisReceiver = IrGetValueImpl(expression.startOffset, expression.endOffset, klass.thisReceiver!!.symbol)

            /**
             * In case of ES6 mode there are a few things done to be aligned with the Kotlin Throwable semantic:
             *
             * 1. If there is a `cause` parameter provided,
             *    it should be put into Error constructor as a field of the second object parameter:
             *    https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Error/Error
             *    It's done by the [setupCauseParameter] function
             *
             * 2. If the `message` parameter is `null`,
             *    we should either use `cause.toString` as a message, or setup message to `undefined`,
             *    because the Error constructor will set it up as an empty string.
             *    It's done by the [setPropertiesToThrowableInstanceSymbol] function
             *
             * 3. Because we should provide the same arguments to the [Error] constructor and to the [setPropertiesToThrowableInstanceSymbol] function
             *    we create temporary variables to hold the values in case of the complex expression
             *    to not evaluate it twice
             */
            if (context.es6mode) {
                var delegatingCall = expression

                val thereIsAnOverrideOfThrowableMessage = klass.declarations
                    .filterIsInstanceAnd<IrSimpleFunction> { !it.isFakeOverride }
                    .any { function ->
                        val property = function.correspondingPropertySymbol?.owner ?: return@any false
                        property.name == Name.identifier("message") &&
                                property.collectRealOverrides(filter = { it.parent == throwableClass.owner }).isNotEmpty()
                    }

                val messageTmp = JsIrBuilder.buildVar(
                    initializer = messageArg ?: undefinedValue(),
                    type = context.dynamicType,
                    parent = currentConstructor,
                    origin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE
                )

                val causeTmp = JsIrBuilder.buildVar(
                    initializer = causeArg ?: undefinedValue(),
                    type = context.dynamicType,
                    parent = currentConstructor,
                    origin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE
                )
                if (causeArg != null) {
                    val throwableCtorWithCause = throwableConstructors.first { it.owner.parameters.size == 2 }

                    if (delegatingCall.symbol != throwableCtorWithCause) {
                        delegatingCall = JsIrBuilder.buildDelegatingConstructorCall(
                            throwableCtorWithCause,
                            startOffset = expression.startOffset,
                            endOffset = expression.endOffset
                        )
                    }

                    delegatingCall.arguments[1] = JsIrBuilder.buildCall(setupCauseParameter).apply {
                        arguments[0] = JsIrBuilder.buildGetValue(causeTmp.symbol)
                    }
                }

                if (messageArg != null) {
                    delegatingCall.arguments[0] = JsIrBuilder.buildGetValue(messageTmp.symbol)
                }

                return JsIrBuilder.buildComposite(
                    context.irBuiltIns.unitType,
                    listOf(
                        messageTmp,
                        causeTmp,
                        delegatingCall,
                        JsIrBuilder.buildCall(setPropertiesToThrowableInstanceSymbol).apply {
                            arguments[0] = thisReceiver
                            arguments[1] = when {
                                thereIsAnOverrideOfThrowableMessage -> JsIrBuilder.buildString(context.irBuiltIns.stringType, "")
                                causeArg != null || messageArg != null -> JsIrBuilder.buildGetValue(messageTmp.symbol)
                                else -> undefinedValue()
                            }
                            arguments[2] =
                                if (causeArg != null) JsIrBuilder.buildGetValue(causeTmp.symbol) else undefinedValue()
                        }
                    )
                )
            }

            val expressionReplacement = expression.run {
                IrCallImpl(
                    startOffset, endOffset, type, extendThrowableFunction,
                    typeArgumentsCount = 0
                ).apply {
                    arguments[0] = thisReceiver
                    arguments[1] = messageArg ?: undefinedValue()
                    arguments[2] = causeArg ?: undefinedValue()
                }
            }

            return JsIrBuilder.buildComposite(
                context.irBuiltIns.unitType,
                listOf(
                    IrDelegatingConstructorCallImpl(
                        expression.startOffset,
                        expression.endOffset,
                        context.irBuiltIns.anyType,
                        anyConstructor,
                        0,
                    ),
                    expressionReplacement
                )
            )
        }
    }
}