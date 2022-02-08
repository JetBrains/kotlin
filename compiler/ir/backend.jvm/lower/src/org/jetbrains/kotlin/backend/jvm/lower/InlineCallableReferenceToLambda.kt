/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.JvmLoweredStatementOrigin
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.irArray
import org.jetbrains.kotlin.backend.jvm.ir.isInlineFunctionCall
import org.jetbrains.kotlin.backend.jvm.ir.isInlineParameter
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addExtensionReceiver
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name

internal val inlineCallableReferenceToLambdaPhase = makeIrFilePhase(
    ::InlineCallableReferenceToLambdaPhase,
    name = "InlineCallableReferenceToLambdaPhase",
    description = "Transform callable reference to inline lambdas, mark inline lambdas for later passes"
)

// This lowering transforms CR passed to inline function to lambda which would be inlined
//
//      inline fun foo(inlineParameter: (A) -> B): B {
//          return inlineParameter()
//      }
//
//      foo(::smth) -> foo { a -> smth(a) }
//
internal class InlineCallableReferenceToLambdaPhase(val context: JvmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) =
        irFile.accept(InlineCallableReferenceToLambdaVisitor(context), null)
}

const val STUB_FOR_INLINING = "stub_for_inlining"

private class InlineCallableReferenceToLambdaVisitor(val context: JvmBackendContext) : IrElementVisitor<Unit, IrDeclaration?> {
    override fun visitElement(element: IrElement, data: IrDeclaration?) =
        element.acceptChildren(this, element as? IrDeclaration ?: data)

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression, data: IrDeclaration?) {
        expression.acceptChildren(this, data)
        val function = expression.symbol.owner
        if (function.isInlineFunctionCall(context)) {
            for (parameter in function.valueParameters) {
                if (parameter.isInlineParameter()) {
                    expression.putValueArgument(parameter.index, expression.getValueArgument(parameter.index)?.transform(data))
                }
            }
        }
    }

    private fun IrExpression.transform(scope: IrDeclaration?) = when {
        this is IrBlock && origin.isInlinable -> apply {
            // Already a lambda or similar, just mark it with an origin.
            val reference = statements.last() as IrFunctionReference
            reference.symbol.owner.origin = JvmLoweredDeclarationOrigin.INLINE_LAMBDA
            statements[statements.lastIndex] = reference.replaceOrigin(JvmLoweredStatementOrigin.INLINE_LAMBDA)
        }

        this is IrFunctionReference -> // ::function -> { args... -> function(args...) }
            wrapFunction(symbol.owner).toLambda(this, scope!!)

        this is IrPropertyReference -> // ::property -> { receiver -> receiver.property }; prefer direct field access if allowed.
            (if (field != null) wrapField(field!!.owner) else wrapFunction(getter!!.owner)).toLambda(this, scope!!)

        else -> this // not an inline argument
    }

    private fun IrPropertyReference.wrapField(field: IrField): IrSimpleFunction =
        context.irFactory.buildFun {
            setSourceRange(this@wrapField)
            origin = JvmLoweredDeclarationOrigin.INLINE_LAMBDA
            name = Name.identifier(STUB_FOR_INLINING)
            visibility = DescriptorVisibilities.LOCAL
            returnType = field.type
        }.apply {
            body = context.createIrBuilder(symbol).run {
                val boundReceiver = dispatchReceiver ?: extensionReceiver
                val fieldReceiver = when {
                    field.isStatic -> null
                    boundReceiver != null -> irGet(addExtensionReceiver(boundReceiver.type))
                    else -> irGet(addValueParameter("receiver", field.parentAsClass.defaultType))
                }
                irExprBody(irGetField(fieldReceiver, field))
            }
        }

    private fun IrCallableReference<*>.wrapFunction(referencedFunction: IrFunction): IrSimpleFunction =
        context.irFactory.buildFun {
            setSourceRange(this@wrapFunction)
            origin = JvmLoweredDeclarationOrigin.INLINE_LAMBDA
            name = Name.identifier(STUB_FOR_INLINING)
            visibility = DescriptorVisibilities.LOCAL
            returnType = ((type as IrSimpleType).arguments.last() as IrTypeProjection).type
            isSuspend = referencedFunction.isSuspend
        }.apply {
            body = context.createJvmIrBuilder(symbol, startOffset, endOffset).run {
                // TODO: could there be a star projection here?
                val argumentTypes = (type as IrSimpleType).arguments.dropLast(1).map { (it as IrTypeProjection).type }
                val boundReceiver = dispatchReceiver ?: extensionReceiver
                val boundReceiverParameter = when {
                    dispatchReceiver != null -> referencedFunction.dispatchReceiverParameter
                    extensionReceiver != null -> referencedFunction.extensionReceiverParameter
                    else -> null
                }
                irExprBody(irCall(referencedFunction.symbol, returnType).apply {
                    copyTypeArgumentsFrom(this@wrapFunction)
                    for (parameter in referencedFunction.explicitParameters) {
                        val next = valueParameters.size
                        when {
                            boundReceiverParameter == parameter ->
                                irGet(addExtensionReceiver(boundReceiver!!.type))
                            parameter.isVararg && next < argumentTypes.size && parameter.type == argumentTypes[next] ->
                                irGet(addValueParameter("p$next", argumentTypes[next]))
                            parameter.isVararg && (next < argumentTypes.size || !parameter.hasDefaultValue()) ->
                                irArray(parameter.type) {
                                    for (i in next until argumentTypes.size) {
                                        +irGet(addValueParameter("p$i", argumentTypes[i]))
                                    }
                                }
                            next >= argumentTypes.size ->
                                null
                            else ->
                                irGet(addValueParameter("p$next", argumentTypes[next]))
                        }?.let { putArgument(referencedFunction, parameter, it) }
                    }
                })
            }
        }

    private fun IrSimpleFunction.toLambda(original: IrCallableReference<*>, scope: IrDeclaration) =
        context.createIrBuilder(scope.symbol).irBlock(startOffset, endOffset, IrStatementOrigin.LAMBDA) {
            this@toLambda.parent = parent
            +this@toLambda
            +IrFunctionReferenceImpl.fromSymbolOwner(
                startOffset, endOffset, original.type, symbol, typeArgumentsCount = 0, reflectionTarget = null,
                origin = JvmLoweredStatementOrigin.INLINE_LAMBDA
            ).apply {
                copyAttributes(original)
                extensionReceiver = original.dispatchReceiver ?: original.extensionReceiver
            }
        }
}

private val IrStatementOrigin?.isInlinable: Boolean
    get() = isLambda || this == IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE || this == IrStatementOrigin.SUSPEND_CONVERSION

private fun IrFunctionReference.replaceOrigin(origin: IrStatementOrigin): IrFunctionReference =
    IrFunctionReferenceImpl(startOffset, endOffset, type, symbol, typeArgumentsCount, valueArgumentsCount, reflectionTarget, origin).also {
        it.copyAttributes(this)
        it.copyTypeAndValueArgumentsFrom(this)
    }
