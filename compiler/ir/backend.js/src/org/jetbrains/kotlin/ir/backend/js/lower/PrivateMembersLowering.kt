/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedValueParameterDescriptor
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrPropertyReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

private val STATIC_THIS_PARAMETER = object : IrDeclarationOriginImpl("STATIC_THIS_PARAMETER") {}

class PrivateMembersLowering(val context: JsIrBackendContext) : FileLoweringPass {
    private val memberMap = context.memberMap

    override fun lower(irFile: IrFile) {
        transformPrivateDeclarations(irFile)
        transformPrivateUseSites(irFile)
    }

    private fun transformPrivateDeclarations(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitClass(declaration: IrClass): IrStatement {
                declaration.transformChildrenVoid(this)
                declaration.declarations.transformFlat {
                    when (it) {
                        is IrSimpleFunction -> transformMemberToStaticFunction(it)?.let { staticFunction ->
                            listOf(staticFunction)
                        }
                        is IrProperty -> listOf(it.apply {
                            this.getter = this.getter?.let { g -> transformAccessor(g) }
                            this.setter = this.setter?.let { s -> transformAccessor(s) }
                        })
                        else -> null
                    }
                }
                return declaration
            }
        })
    }

    private fun transformPrivateUseSites(irFile: IrFile) {
        irFile.transform(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                super.visitCall(expression)

                return getOrPutStaticFunction(expression.symbol)?.let {
                    transformPrivateToStaticCall(expression, it)
                } ?: expression
            }

            override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
                super.visitFunctionReference(expression)

                return getOrPutStaticFunction(expression.symbol)?.let {
                    transformPrivateToStaticReference(expression) {
                        IrFunctionReferenceImpl(
                            expression.startOffset, expression.endOffset,
                            expression.type,
                            it.symbol, expression.typeArgumentsCount,
                            expression.valueArgumentsCount, expression.origin
                        )
                    }
                } ?: expression
            }

            override fun visitPropertyReference(expression: IrPropertyReference): IrExpression {
                super.visitPropertyReference(expression)

                val staticGetter = expression.getter?.let { getOrPutStaticFunction(it) }
                val staticSetter = expression.setter?.let { getOrPutStaticFunction(it) }

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

    private fun transformAccessor(accessor: IrSimpleFunction) = transformMemberToStaticFunction(accessor) ?: accessor

    private fun transformMemberToStaticFunction(function: IrSimpleFunction): IrSimpleFunction? {

        val staticFunction = getOrPutStaticFunction(function.symbol) ?: return null

        // Detach old function from corresponding property
        val correspondingProperty = function.correspondingPropertySymbol?.owner
        if (correspondingProperty != null) {
            when (function) {
                correspondingProperty.getter -> correspondingProperty.getter = staticFunction
                correspondingProperty.setter -> correspondingProperty.setter = staticFunction
            }
        }

        val oldParameters =
            listOfNotNull(function.extensionReceiverParameter, function.dispatchReceiverParameter) + function.valueParameters
        val newParameters = listOfNotNull(staticFunction.extensionReceiverParameter) + staticFunction.valueParameters
        assert(oldParameters.size == newParameters.size)

        val parameterMapping = oldParameters.zip(newParameters).toMap()

        staticFunction.body = function.body?.deepCopyWithSymbols(staticFunction)

        staticFunction.transform(object : IrElementTransformerVoid() {
            override fun visitGetValue(expression: IrGetValue) = parameterMapping[expression.symbol.owner]?.let {
                expression.run { IrGetValueImpl(startOffset, endOffset, type, it.symbol, origin) }
            } ?: expression
        }, null)

        return staticFunction
    }

    private fun getOrPutStaticFunction(functionSymbol: IrFunctionSymbol): IrSimpleFunction? {
        val function = functionSymbol.owner
        if (function !is IrSimpleFunction) return null
        if (function.visibility != Visibilities.PRIVATE || function.dispatchReceiverParameter == null) return null

        return memberMap.getOrPut(function.symbol) {
            val descriptor = WrappedSimpleFunctionDescriptor()
            val symbol = IrSimpleFunctionSymbolImpl(descriptor)
            val staticFunction = function.run {
                IrFunctionImpl(
                    startOffset, endOffset, origin,
                    symbol, name, visibility, modality,
                    returnType,
                    isInline = isInline, isExternal = isExternal, isTailrec = isTailrec, isSuspend = isSuspend, isExpect = isExpect,
                    isFakeOverride = origin == IrDeclarationOrigin.FAKE_OVERRIDE
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

            staticFunction.valueParameters += function.valueParameters.map { it.copyTo(staticFunction, index = it.index + 1) }

            staticFunction
        }
    }

}