/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend.lower

import org.jetbrains.kotlin.backend.common.lower.LoweredDeclarationOrigins
import org.jetbrains.kotlin.backend.common.lower.LoweredStatementOrigins
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.JvmLoweredStatementOrigin
import org.jetbrains.kotlin.bir.backend.BirLoweringPhase
import org.jetbrains.kotlin.bir.backend.builders.*
import org.jetbrains.kotlin.bir.backend.jvm.JvmBirBackendContext
import org.jetbrains.kotlin.bir.backend.jvm.birArray
import org.jetbrains.kotlin.bir.backend.jvm.isInlineFunctionCall
import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.declarations.impl.BirSimpleFunctionImpl
import org.jetbrains.kotlin.bir.expressions.*
import org.jetbrains.kotlin.bir.getBackReferences
import org.jetbrains.kotlin.bir.types.BirSimpleType
import org.jetbrains.kotlin.bir.types.BirTypeProjection
import org.jetbrains.kotlin.bir.util.*
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name

context(JvmBirBackendContext)
class BirInlineCallableReferenceToLambdaLowering : BirLoweringPhase() {
    override fun lower(module: BirModuleFragment) {
        getAllElementsOfClass(BirFunction, true).forEach { function ->
            if (function.isInlineFunctionCall()) {
                val accesses by lazy { function.getBackReferences(BirFunctionAccessExpression.symbol) }
                for (parameter in function.valueParameters) {
                    if (parameter.isInlineParameter()) {
                        accesses.forEach { access ->
                            transferInlineArgument(access.valueArguments[parameter.index])
                        }
                    }
                }
            }
        }
    }

    private fun transferInlineArgument(argument: BirExpression?) {
        when {
            argument is BirBlock && argument.origin.isInlinable -> {
                // Already a lambda or similar, just mark it with an origin.
                val reference = argument.statements.last() as BirFunctionReference
                reference.symbol.owner.origin = LoweredDeclarationOrigins.INLINE_LAMBDA
                reference.origin = LoweredStatementOrigins.INLINE_LAMBDA
            }
            argument is BirFunctionReference -> {
                // ::function -> { args... -> function(args...) }
                val new = toLambda(wrapFunction(argument, argument.symbol.owner), argument)
                argument.replaceWith(new)
            }
            argument is BirPropertyReference -> {
                val isReferenceToSyntheticJavaProperty = argument.symbol.owner.origin.let {
                    it == IrDeclarationOrigin.SYNTHETIC_JAVA_PROPERTY_DELEGATE || it == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
                }
                val new = when {
                    // References to generic synthetic Java properties aren't inlined in K1. Fixes KT-57103
                    argument.typeArguments.isNotEmpty() && isReferenceToSyntheticJavaProperty -> argument
                    // ::property -> { receiver -> receiver.property }; prefer direct field access if allowed.
                    argument.field != null -> wrapField(argument, argument.field!!.owner)
                    else -> toLambda(wrapFunction(argument, argument.getter!!.owner), argument)
                }
                argument.replaceWith(new)
            }
            else -> {}
        }
    }

    private fun wrapField(reference: BirPropertyReference, field: BirField): BirSimpleFunctionImpl {
        return BirSimpleFunction.build {
            sourceSpan = reference.sourceSpan
            origin = LoweredDeclarationOrigins.INLINE_LAMBDA
            name = Name.identifier(STUB_FOR_INLINING)
            visibility = DescriptorVisibilities.LOCAL
            returnType = field.type
            body = birExpressionBody {
                val boundReceiver = reference.dispatchReceiver ?: reference.extensionReceiver
                val fieldReceiver = when {
                    field.isStatic -> null
                    boundReceiver != null -> birGet(createBirExtensionReceiver().apply {
                        type = boundReceiver.type
                        extensionReceiverParameter = this
                    })
                    else -> birGet(BirValueParameter.build {
                        name = Name.identifier("receiver")
                        type = field.parentAsClass.defaultType
                        valueParameters += this
                    })
                }
                birGetField(fieldReceiver, field)
            }
        }
    }

    private fun wrapFunction(reference: BirCallableReference<*>, referencedFunction: BirFunction): BirSimpleFunction {
        // TODO: could there be a star projection here?
        val argumentTypes = (reference.type as BirSimpleType).arguments.dropLast(1).map { (it as BirTypeProjection).type }
        val boundReceiver = reference.dispatchReceiver ?: reference.extensionReceiver
        val boundReceiverParameter = when {
            reference.dispatchReceiver != null -> referencedFunction.dispatchReceiverParameter
            reference.extensionReceiver != null -> referencedFunction.extensionReceiverParameter
            else -> null
        }

        return BirSimpleFunction.build {
            sourceSpan = reference.sourceSpan
            origin = LoweredDeclarationOrigins.INLINE_LAMBDA
            name = Name.identifier(STUB_FOR_INLINING)
            visibility = DescriptorVisibilities.LOCAL
            returnType = ((reference.type as BirSimpleType).arguments.last() as BirTypeProjection).type
            isSuspend = referencedFunction is BirSimpleFunction && referencedFunction.isSuspend
            val wrapperFunction = this
            body = birExpressionBody {
                birCallFunctionOrConstructor(referencedFunction.symbol, wrapperFunction.returnType) {
                    typeArguments = reference.typeArguments
                    for (parameter in referencedFunction.explicitParameters) {
                        val next = valueParameters.size
                        val argument = when {
                            boundReceiverParameter == parameter ->
                                birGet(createBirExtensionReceiver().apply {
                                    type = boundReceiver!!.type
                                    wrapperFunction.extensionReceiverParameter = this
                                })
                            parameter.isVararg && next < argumentTypes.size && parameter.type == argumentTypes[next] ->
                                birGet(BirValueParameter.build {
                                    name = Name.identifier("p$next")
                                    type = argumentTypes[next]
                                    index = wrapperFunction.valueParameters.size
                                    wrapperFunction.valueParameters += this
                                })
                            parameter.isVararg && (next < argumentTypes.size || !parameter.hasDefaultValue()) ->
                                error("Callable reference with vararg should not appear at this stage.\n${reference.render()}")
                            next >= argumentTypes.size ->
                                null
                            else ->
                                birGet(BirValueParameter.build {
                                    name = Name.identifier("p$next")
                                    type = argumentTypes[next]
                                    index = wrapperFunction.valueParameters.size
                                    wrapperFunction.valueParameters += this
                                })
                        }

                        if (argument != null) {
                            putArgument(parameter, argument)
                        }
                    }
                }
            }
        }
    }

    private fun toLambda(function: BirSimpleFunction, original: BirCallableReference<*>): BirContainerExpression {
        return birBodyScope {
            sourceSpan = function.sourceSpan
            origin = IrStatementOrigin.LAMBDA
            birBlock {
                +function
                +birFunctionReference(function, original.type, origin = LoweredStatementOrigins.INLINE_LAMBDA) {
                    copyAttributes(original)
                    extensionReceiver = original.dispatchReceiver ?: original.extensionReceiver
                }
            }
        }
    }


    private val IrStatementOrigin?.isInlinable: Boolean
        get() = isLambda || this == IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE || this == IrStatementOrigin.SUSPEND_CONVERSION

    companion object {
        const val STUB_FOR_INLINING = "stub_for_inlining"
    }
}