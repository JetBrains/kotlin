/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedValueParameterDescriptor
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.Name

private val STATIC_THIS_PARAMETER = object : IrDeclarationOriginImpl("STATIC_THIS_PARAMETER") {}

class PrivateMembersLowering(val context: JsIrBackendContext) : DeclarationTransformer {

    private var IrFunction.correspondingStatic by context.mapping.privateMemberToCorrespondingStatic

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        return when (declaration) {
            is IrSimpleFunction -> transformMemberToStaticFunction(declaration)?.let { staticFunction ->
                declaration.correspondingStatic = staticFunction
                listOf(staticFunction)
            }
            is IrProperty -> listOf(declaration.apply {
                // Detach old function from corresponding property
                this.getter = this.getter?.let { g -> transformAccessor(g) }
                this.setter = this.setter?.let { s -> transformAccessor(s) }
            })
            else -> null
        }
    }

    private fun transformAccessor(accessor: IrSimpleFunction) = transformMemberToStaticFunction(accessor) ?: accessor

    private fun transformMemberToStaticFunction(function: IrSimpleFunction): IrSimpleFunction? {

        if (function.visibility != Visibilities.PRIVATE || function.dispatchReceiverParameter == null) return null

        val descriptor = WrappedSimpleFunctionDescriptor()
        val symbol = IrSimpleFunctionSymbolImpl(descriptor)
        val staticFunction = function.run {
            IrFunctionImpl(
                startOffset, endOffset, origin,
                symbol, name, visibility, modality,
                returnType,
                isInline = isInline, isExternal = isExternal, isTailrec = isTailrec, isSuspend = isSuspend, isExpect = isExpect,
                isFakeOverride = isFakeOverride,
                isOperator = isOperator
            ).also {
                descriptor.bind(it)
                it.parent = parent
                it.correspondingPropertySymbol = correspondingPropertySymbol
            }
        }

        staticFunction.typeParameters += function.typeParameters.map { it.deepCopyWithSymbols(staticFunction) }

        staticFunction.extensionReceiverParameter = function.extensionReceiverParameter?.copyTo(staticFunction)
        val thisDesc = WrappedValueParameterDescriptor()
        val thisSymbol = IrValueParameterSymbolImpl(thisDesc)
        staticFunction.valueParameters += IrValueParameterImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            STATIC_THIS_PARAMETER,
            thisSymbol,
            Name.identifier("\$this"),
            0,
            function.dispatchReceiverParameter!!.type,
            null,
            isCrossinline = false,
            isNoinline = false
        ).also {
            thisDesc.bind(it)
            it.parent = staticFunction
        }

        function.correspondingStatic = staticFunction

        staticFunction.valueParameters += function.valueParameters.map {
            // TODO better way to avoid copying default value
            it.copyTo(staticFunction, index = it.index + 1, defaultValue = null)
        }

        val oldParameters =
            listOfNotNull(function.extensionReceiverParameter, function.dispatchReceiverParameter) + function.valueParameters
        val newParameters = listOfNotNull(staticFunction.extensionReceiverParameter) + staticFunction.valueParameters
        assert(oldParameters.size == newParameters.size)

        val parameterMapping = oldParameters.zip(newParameters).toMap()

        val parameterTransformer = object : IrElementTransformerVoid() {
            override fun visitGetValue(expression: IrGetValue) = parameterMapping[expression.symbol.owner]?.let {
                expression.run { IrGetValueImpl(startOffset, endOffset, type, it.symbol, origin) }
            } ?: expression
        }

        fun IrBody.copyWithParameters(): IrBody {
            return deepCopyWithSymbols(staticFunction).also {
                it.transform(parameterTransformer, null)
            }
        }

        function.valueParameters.forEach {
            // TODO better way to avoid copying default value

            parameterMapping[it]?.apply {
                it.defaultValue?.let { originalDefault ->
                    defaultValue = IrExpressionBodyImpl(it.startOffset, it.endOffset) {
                        expression = (originalDefault.copyWithParameters() as IrExpressionBody).expression
                    }
                }
            }
        }

        function.body?.let {
            staticFunction.body = when (it) {
                is IrBlockBody -> IrBlockBodyImpl(it.startOffset, it.endOffset) {
                    statements += (it.copyWithParameters() as IrBlockBody).statements
                }
                is IrExpressionBody -> IrExpressionBodyImpl(it.startOffset, it.endOffset) {
                    expression = (it.copyWithParameters() as IrExpressionBody).expression
                }
                is IrSyntheticBody -> it
                else -> error("Unexpected body kind: ${it.javaClass}")
            }
        }

        return staticFunction
    }
}

class PrivateMemberBodiesLowering(val context: JsIrBackendContext) : BodyLoweringPass {

    private var IrFunction.correspondingStatic by context.mapping.privateMemberToCorrespondingStatic

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transform(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                super.visitCall(expression)

                return expression.symbol.owner.correspondingStatic?.let {
                    transformPrivateToStaticCall(expression, it)
                } ?: expression
            }

            override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
                super.visitFunctionReference(expression)

                return expression.symbol.owner.correspondingStatic?.let {
                    transformPrivateToStaticReference(expression) {
                        IrFunctionReferenceImpl(
                            expression.startOffset, expression.endOffset,
                            expression.type,
                            it.symbol, expression.typeArgumentsCount,
                            expression.valueArgumentsCount, expression.reflectionTarget, expression.origin
                        )
                    }
                } ?: expression
            }

            override fun visitPropertyReference(expression: IrPropertyReference): IrExpression {
                super.visitPropertyReference(expression)

                val staticGetter = expression.getter?.owner?.correspondingStatic
                val staticSetter = expression.setter?.owner?.correspondingStatic

                return if (staticGetter != null || staticSetter != null) {
                    transformPrivateToStaticReference(expression) {
                        IrPropertyReferenceImpl(
                            expression.startOffset, expression.endOffset,
                            expression.type,
                            expression.symbol, // TODO remap property symbol based on remapped getter/setter?
                            expression.typeArgumentsCount,
                            expression.field,
                            staticGetter?.symbol ?: expression.getter,
                            staticSetter?.symbol ?: expression.setter,
                            expression.origin
                        )
                    }
                } else expression
            }

            private fun transformPrivateToStaticCall(expression: IrCall, staticTarget: IrSimpleFunction): IrCall {
                val newExpression = IrCallImpl(
                    expression.startOffset, expression.endOffset,
                    expression.type,
                    staticTarget.symbol, expression.typeArgumentsCount,
                    expression.origin,
                    expression.superQualifierSymbol
                )

                newExpression.extensionReceiver = expression.extensionReceiver
                expression.dispatchReceiver?.let { newExpression.putValueArgument(0, it) }

                for (i in 0 until expression.valueArgumentsCount) {
                    newExpression.putValueArgument(i + 1, expression.getValueArgument(i))
                }
                newExpression.copyTypeArgumentsFrom(expression)

                return newExpression
            }

            private fun transformPrivateToStaticReference(
                expression: IrCallableReference,
                builder: () -> IrCallableReference
            ): IrCallableReference {

                val newExpression = builder()

                newExpression.extensionReceiver = expression.extensionReceiver

                newExpression.dispatchReceiver = expression.dispatchReceiver
                for (i in 0 until expression.valueArgumentsCount) {
                    newExpression.putValueArgument(i, expression.getValueArgument(i))
                }
                newExpression.copyTypeArgumentsFrom(expression)

                return newExpression
            }
        }, null)
    }
}