/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.descriptors.WrappedValueParameterDescriptor
import org.jetbrains.kotlin.backend.common.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.ir.isSuspend
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.codegen.coroutines.COROUTINE_LABEL_FIELD_NAME
import org.jetbrains.kotlin.codegen.coroutines.INVOKE_SUSPEND_METHOD_NAME
import org.jetbrains.kotlin.codegen.coroutines.dataFieldName
import org.jetbrains.kotlin.codegen.coroutines.getOrCreateJvmSuspendFunctionView
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.declarations.buildConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
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
import org.jetbrains.kotlin.load.java.JavaVisibilities
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.OperatorNameConventions
import java.util.*

// TODO: Move to common prefix
internal val addContinuationPhase = makeIrFilePhase(
    ::AddContinuationLowering,
    "AddContinuation",
    "Add continuation parameter to suspend functions and suspend calls"
)

private object CONTINUATION_PARAMETER : IrDeclarationOriginImpl("CONTINUATION_PARAMETER")
private object CONTINUATION_CLASS : IrDeclarationOriginImpl("CONTINUATION_CLASS")
private object CONTINUATION_CLASS_CONSTRUCTOR : IrDeclarationOriginImpl("CONTINUATION_CLASS_CONSTRUCTOR")
private object CONTINUATION_CLASS_COMPLETION_PARAMETER : IrDeclarationOriginImpl("CONTINUATION_CLASS_COMPLETION_PARAMETER")
private object CONTINUATION_CLASS_INVOKE_SUSPEND : IrDeclarationOriginImpl("CONTINUATION_CLASS_INVOKE_SUSPEND")
private object CONTINUATION_CLASS_RESULT_FIELD : IrDeclarationOriginImpl("CONTINUATION_CLASS_RESULT_FIELD")
private object CONTINUATION_CLASS_LABEL_FIELD : IrDeclarationOriginImpl("CONTINUATION_CLASS_LABEL_FIELD")

private class AddContinuationLowering(private val context: JvmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        val suspendLambdas = markSuspendLambdas(irFile)
        val suspendFunctions = addContinuationParameter(irFile, suspendLambdas)
        transformSuspendCalls(irFile, suspendLambdas)
        suspendFunctions.forEach {
            generateContinuationClass(it)
        }
        // TODO: suspend lambdas are covered by CallableReferenceLowering, which is not ideal
    }

    // TODO: move to separate lowering and make the lowering common
    private fun generateContinuationClass(irFunction: IrFunction) {
        val continuationImpl = context.continuationImpl.owner
        val continuationClass = buildClass {
            // TODO: Use the same counter as CallableReferenceLowering
            name = "${irFunction.name}\$1".synthesizedName
            origin = CONTINUATION_CLASS
            visibility = JavaVisibilities.PACKAGE_VISIBILITY
        }.apply {
            createImplicitParameterDeclarationWithWrappedDescriptor()
            parent = irFunction.parent
            superTypes.add(continuationImpl.defaultType)

            (parent as IrDeclarationContainer).declarations.add(this)
        }

        val resultField = buildField {
            origin = CONTINUATION_CLASS_RESULT_FIELD
            name = Name.identifier(context.state.languageVersionSettings.dataFieldName())
            type = context.irBuiltIns.anyType
            isFinal = false
            visibility = JavaVisibilities.PACKAGE_VISIBILITY
        }.apply {
            parent = continuationClass
            continuationClass.declarations.add(this)
        }

        val labelField = buildField {
            origin = CONTINUATION_CLASS_LABEL_FIELD
            name = Name.identifier(COROUTINE_LABEL_FIELD_NAME)
            type = context.irBuiltIns.intType
            isFinal = false
            visibility = JavaVisibilities.PACKAGE_VISIBILITY
        }.apply {
            parent = continuationClass
            continuationClass.declarations.add(this)
        }

        // TODO: How the fuck do I disable parameter assertions generation????
        buildConstructor {
            isPrimary = true
            origin = CONTINUATION_CLASS_CONSTRUCTOR
            visibility = Visibilities.PUBLIC
            returnType = continuationClass.defaultType
        }.apply {
            parent = continuationClass
            continuationClass.declarations.add(this)

            val completionParameterDescriptor = WrappedValueParameterDescriptor()
            val completionParameterSymbol = IrValueParameterSymbolImpl(completionParameterDescriptor)
            valueParameters.add(
                0, IrValueParameterImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    CONTINUATION_CLASS_COMPLETION_PARAMETER,
                    completionParameterSymbol,
                    Name.identifier("completion"),
                    0,
                    context.continuationClass.owner.defaultType,
                    null,
                    isCrossinline = false, isNoinline = false
                ).also { completionParameterDescriptor.bind(it) }
            )

            val superClassConstructor = continuationImpl.constructors.single { it.valueParameters.size == 1 }
            body = context.createIrBuilder(symbol).irBlockBody {
                +irDelegatingConstructorCall(superClassConstructor).also {
                    it.putValueArgument(0, irGet(completionParameterSymbol.owner))
                }
            }
        }

        val thisReceiver = continuationClass.thisReceiver!!

        val invokeSuspendName = Name.identifier(INVOKE_SUSPEND_METHOD_NAME)
        val invokeSuspend = continuationImpl.functions.single { it.name == invokeSuspendName }.symbol
        buildFun {
            origin = CONTINUATION_CLASS_INVOKE_SUSPEND
            name = invokeSuspendName
            visibility = Visibilities.PUBLIC
            isSuspend = false
            returnType = context.irBuiltIns.anyType
        }.apply {
            parent = continuationClass
            continuationClass.declarations.add(this)
            overriddenSymbols.add(invokeSuspend)
            dispatchReceiverParameter = thisReceiver.copyTo(this)

            val result = invokeSuspend.owner.valueParameters[0].copyTo(this)
            valueParameters.add(result)
            body = context.createIrBuilder(symbol).irBlockBody {
                +irSetField(irGet(dispatchReceiverParameter!!), resultField, irGet(result))
                +irSetField(
                    irGet(dispatchReceiverParameter!!), labelField,
                    // TODO: Why the fuck label is boxed
                    irCallOp(
                        context.irBuiltIns.intClass.functions.single { it.owner.name == OperatorNameConventions.OR },
                        context.irBuiltIns.intType,
                        irGetField(irGet(dispatchReceiverParameter!!), labelField),
                        irInt(1 shl 31)
                    )
                )
                +irReturn(irCall(irFunction).also { it.putValueArgument(0, irGet(dispatchReceiverParameter!!)) })
            }
        }
    }

    private fun addContinuationParameter(irFile: IrFile, suspendLambdas: List<IrFunction>): Set<IrFunction> {
        val views = hashSetOf<IrFunction>()
        fun tryAddContinuationParameter(element: IrElement): List<IrDeclaration>? {
            return if (element is IrSimpleFunction && element.isSuspend) {
                val view = element.getOrCreateView()
                if (element !in suspendLambdas) {
                    views.add(view)
                }
                listOf(view)
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
        return views
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
                startOffset, endOffset, origin, getOrCreateJvmSuspendFunctionView(descriptor, context.state), context.irBuiltIns.anyType
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
