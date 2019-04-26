/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.descriptors.WrappedValueParameterDescriptor
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.ir.isSuspend
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.codegen.coroutines.getOrCreateJvmSuspendFunctionView
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.createType
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import java.util.*

// TODO: Move to common prefix
internal val addContinuationPhase = makeIrFilePhase(
    ::AddContinuationLowering,
    "AddContinuation",
    "Add continuation parameter to suspend functions and suspend calls"
)

private class AddContinuationLowering(private val context: JvmBackendContext) : FileLoweringPass {
    object CONTINUATION_PARAMETER : IrDeclarationOriginImpl("CONTINUATION_PARAMETER")

    override fun lower(irFile: IrFile) {
        val suspendLambdas = markSuspendLambdas(irFile)
        addContinuationParameter(irFile)
        transformSuspendCalls(irFile, suspendLambdas)
    }

    private fun addContinuationParameter(irFile: IrFile) {
        fun tryAddContinuationParameter(element: IrElement): List<IrDeclaration>? {
            return if (element is IrSimpleFunction && element.isSuspend) {
                listOf(element.getOrCreateView())
            } else null
        }

        irFile.transformDeclarationsFlat(::tryAddContinuationParameter)
        irFile.acceptVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitClass(declaration: IrClass) {
                declaration.acceptChildrenVoid(this)
                declaration.transformDeclarationsFlat(::tryAddContinuationParameter)
            }
        })
    }

    private fun markSuspendLambdas(irElement: IrElement): List<IrFunction> {
        val suspendLambdas = arrayListOf<IrFunction>()
        irElement.acceptChildrenVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitFunctionReference(expression: IrFunctionReference) {
                expression.acceptChildrenVoid(this)

                if (expression.isSuspend) {
                    suspendLambdas += expression.symbol.owner
                }
            }
        })
        return suspendLambdas
    }

    private fun transformSuspendCalls(irFile: IrFile, suspendLambdas: List<IrFunction>) {
        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            private val functionStack = Stack<IrFunction>()

            override fun visitElement(element: IrElement): IrElement {
                element.transformChildrenVoid(this)
                return element
            }

            override fun visitFunction(declaration: IrFunction): IrStatement {
                functionStack.push(declaration)
                val res = super.visitFunction(declaration)
                functionStack.pop()
                return res
            }

            override fun visitCall(expression: IrCall): IrExpression {
                if (!expression.isSuspend) return super.visitCall(expression)
                val caller = functionStack.peek()
                val continuationArgument = IrGetValueImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    continuationType(caller.returnType),
                    if (caller in suspendLambdas) (caller.dispatchReceiverParameter as IrValueParameter).symbol
                    else caller.valueParameters.last().symbol
                )
                val view = (expression.symbol.owner as IrSimpleFunction).getOrCreateView()
                val res = IrCallImpl(expression.startOffset, expression.endOffset, expression.type, view.symbol).apply {
                    copyTypeArgumentsFrom(expression)
                    dispatchReceiver = expression.dispatchReceiver
                    for (i in 0 until expression.valueArgumentsCount) {
                        putValueArgument(i, expression.getValueArgument(i))
                    }
                    putValueArgument(expression.valueArgumentsCount, continuationArgument)
                }
                return super.visitCall(res)
            }

            override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
                if (!expression.isSuspend) return super.visitFunctionReference(expression)
                val view = (expression.symbol.owner as IrSimpleFunction).getOrCreateView()
                val res = IrFunctionReferenceImpl(
                    expression.startOffset,
                    expression.endOffset,
                    expression.type,
                    view.symbol,
                    view.descriptor,
                    expression.typeArgumentsCount,
                    expression.origin
                ).apply {
                    copyTypeArgumentsFrom(expression)
                    dispatchReceiver = expression.dispatchReceiver
                    for (i in 0 until expression.valueArgumentsCount) {
                        putValueArgument(i, expression.getValueArgument(i))
                    }
                }
                return super.visitFunctionReference(res)
            }
        })
    }

    private fun IrSimpleFunction.getOrCreateView(): IrSimpleFunction =
        context.transformedSuspendFunctionsCache.getOrPut(this) {
            IrFunctionImpl(
                startOffset, endOffset, origin, getOrCreateJvmSuspendFunctionView(descriptor, context.state), returnType
            ).also {
                it.parent = parent
                it.copyTypeParametersFrom(this)

                it.dispatchReceiverParameter = dispatchReceiverParameter?.copyTo(it)

                valueParameters.mapTo(it.valueParameters) { p -> p.copyTo(it) }
                it.valueParameters += createContinuationValueParameter(it.valueParameters.size, returnType).apply { parent = it }

                // Fix links to parameters
                val valueParametersMapping = (valueParameters + dispatchReceiverParameter)
                    .zip(it.valueParameters.dropLast(1) + it.dispatchReceiverParameter).toMap()
                it.body = body?.deepCopyWithSymbols(this)
                it.body?.transformChildrenVoid(object : IrElementTransformerVoid() {
                    override fun visitGetValue(expression: IrGetValue) =
                        valueParametersMapping[expression.symbol.owner]?.let { newParam ->
                            expression.run { IrGetValueImpl(startOffset, endOffset, type, newParam.symbol, origin) }
                        } ?: expression
                })
            }
        }

    private fun createContinuationValueParameter(index: Int, returnType: IrType): IrValueParameter {
        val descriptor = WrappedValueParameterDescriptor()
        return IrValueParameterImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            CONTINUATION_PARAMETER,
            IrValueParameterSymbolImpl(descriptor),
            Name.identifier("\$completion"),
            index,
            continuationType(returnType),
            null, isCrossinline = false, isNoinline = false
        ).also {
            descriptor.bind(it)
        }
    }

    private fun continuationType(returnType: IrType) = context.continuationClass.createType(
        hasQuestionMark = false,
        arguments = listOf(makeTypeProjection(returnType, Variance.INVARIANT))
    )

}