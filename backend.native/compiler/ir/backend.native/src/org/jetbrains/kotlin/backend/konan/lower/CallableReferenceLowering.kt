/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.DeclarationContainerLoweringPass
import org.jetbrains.kotlin.backend.common.descriptors.explicitParameters
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.KonanBackendContext
import org.jetbrains.kotlin.backend.konan.descriptors.getKonanInternalClass
import org.jetbrains.kotlin.backend.konan.descriptors.isFunctionOrKFunctionType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.IrCallableReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.util.addArguments
import org.jetbrains.kotlin.ir.util.createParameterDeclarations
import org.jetbrains.kotlin.ir.util.getArguments
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

/**
 * This lowering pass lowers all [IrCallableReference]s to unbound ones.
 */
internal class CallableReferenceLowering(val context: KonanBackendContext) : DeclarationContainerLoweringPass {

    var callableReferenceCount = 0

    override fun lower(irDeclarationContainer: IrDeclarationContainer) {
        irDeclarationContainer.declarations.transformFlat { declaration ->
            if (declaration !is IrDeclarationContainer)
                lowerCallableReferences(this, irDeclarationContainer, declaration)
            else
                null
        }
    }
}

/**
 * Replaces all callable references in the function body to unbound ones.
 * Returns the list of this function and all created ones.
 */
private fun lowerCallableReferences(lower: CallableReferenceLowering,
                                    irDeclarationContainer: IrDeclarationContainer,
                                    declaration: IrDeclaration): List<IrDeclaration> {
    val containingDeclaration = when (irDeclarationContainer) {
        is IrClass -> irDeclarationContainer.descriptor
        is IrFile -> irDeclarationContainer.packageFragmentDescriptor
        else -> throw AssertionError("Unexpected declaration container: $irDeclarationContainer")
    }
    val transformer = CallableReferencesUnbinder(lower, containingDeclaration)
    declaration.transformChildrenVoid(transformer)
    return listOf(declaration) + transformer.createdFunctions
}

private object DECLARATION_ORIGIN_FUNCTION_FOR_CALLABLE_REFERENCE :
        IrDeclarationOriginImpl("FUNCTION_FOR_CALLABLE_REFERENCE")

/**
 * Replaces all callable references in the function body to unbound ones.
 * Adds all created functions to [createdFunctions].
 */
private class CallableReferencesUnbinder(val lower: CallableReferenceLowering,
                                         val containingDeclaration: DeclarationDescriptor) : IrElementTransformerVoid() {

    val createdFunctions = mutableListOf<IrFunction>()

    private val builtIns = lower.context.builtIns

    override fun visitElement(element: IrElement): IrElement {
        return super.visitElement(element)
    }

    private val unboundCallableReferenceType by lazy {
        builtIns.getKonanInternalClass("UnboundCallableReference").defaultType
    }

    private val simpleFunctionImplClass by lazy {
        builtIns.getKonanInternalClass("SimpleFunctionImpl")
    }

    private val simpleFunctionImplBoundArgsGetter by lazy {
        simpleFunctionImplClass.defaultType.memberScope.getContributedDescriptors()
                .filterIsInstance<PropertyDescriptor>()
                .single { it.name.asString() == "boundArgs" }
                .getter!!
    }

    override fun visitClass(declaration: IrClass): IrStatement {
        // Class is a declaration container - it will be visited by the main visitor (CallableReferenceLowering).
        return declaration
    }

    override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
        expression.transformChildrenVoid(this)
        if (!expression.type.isFunctionOrKFunctionType) {
            // Not a subject of this lowering.
            return expression
        }

        val descriptor = expression.descriptor
        val boundArgs = expression.getArguments()
        val boundParams = boundArgs.map { it.first }

        val allParams = descriptor.explicitParameters
        val unboundParams = allParams - boundParams

        val startOffset = expression.startOffset
        val endOffset = expression.endOffset

        // The code below creates a call to `SimpleFunctionImpl` constructor which creates the object implementing
        // the function type.

        // Create the function to be used as `unboundRef` of `SimpleFunctionImpl`:
        val newFunction = createSimpleFunctionImplTarget(descriptor, unboundParams, boundParams, startOffset, endOffset)

        createdFunctions.add(newFunction)

        // Create the call to constructor and its arguments:

        val unboundRef = IrFunctionReferenceImpl(startOffset, endOffset,
                unboundCallableReferenceType, newFunction.descriptor, null)
        val simpleFunctionImplClassConstructor = simpleFunctionImplClass.unsubstitutedPrimaryConstructor!!

        val boundArgsVarargParam = simpleFunctionImplClassConstructor.valueParameters[1]
        val boundArgsVararg = IrVarargImpl(
                startOffset, endOffset,
                boundArgsVarargParam.type,
                boundArgsVarargParam.varargElementType!!,
                boundArgs.map { it.second }
        )

        return IrCallImpl(startOffset, endOffset, simpleFunctionImplClassConstructor).apply {
            putValueArgument(0, unboundRef)
            putValueArgument(1, boundArgsVararg)
        }
    }

    /**
     * For given function [descriptor] creates the function which calls [descriptor] but
     * takes all [boundParams] packed into `SimpleFunctionImpl` (and all [unboundParams] as is).
     */
    private fun createSimpleFunctionImplTarget(descriptor: FunctionDescriptor,
                                               unboundParams: List<ParameterDescriptor>,
                                               boundParams: List<ParameterDescriptor>,
                                               startOffset: Int, endOffset: Int): IrFunctionImpl {

        // Create new function descriptor:
        val newDescriptor = createSimpleFunctionImplTargetDescriptor(descriptor, unboundParams)

        val builder = lower.context.createIrBuilder(newDescriptor)
        val blockBody = builder.irBlockBody(startOffset, endOffset) {
            val boundArgsGet = irCall(simpleFunctionImplBoundArgsGetter).apply {
                dispatchReceiver = irGet(newDescriptor.valueParameters[0])
            }

            +irLet(boundArgsGet) { boundArgs ->
                val arrayGet = boundArgs.type.memberScope.getContributedDescriptors()
                        .filterIsInstance<FunctionDescriptor>().single { it.name.asString() == "get" }

                // TODO: some handling for type parameters is probably required here.

                // Call to old function from new function:
                val call = irCall(descriptor).apply {
                    // unboundParams are received as all new function parameters except first:
                    val newUnboundParams = newDescriptor.valueParameters.drop(1)
                    assert (unboundParams.size == newUnboundParams.size)
                    addArguments(unboundParams.mapIndexed { index, param ->
                        param to irGet(newUnboundParams[index])
                    })

                    // boundParams are received in `SimpleFunctionImpl.boundArgs`:
                    addArguments(boundParams.mapIndexed { index, param ->
                        param to irCall(arrayGet).apply {
                            dispatchReceiver = irGet(boundArgs)
                            putValueArgument(0, irInt(index))
                        }
                    })
                }

                irReturn(call)
            }
        }

        val newFunction = IrFunctionImpl(
                startOffset, endOffset, DECLARATION_ORIGIN_FUNCTION_FOR_CALLABLE_REFERENCE,
                newDescriptor,
                blockBody
        )

        newFunction.createParameterDeclarations()

        return newFunction
    }

    /**
     * Creates descriptor for [createSimpleFunctionImplTarget].
     */
    private fun createSimpleFunctionImplTargetDescriptor(descriptor: CallableDescriptor,
                                                         unboundParams: List<ParameterDescriptor>): FunctionDescriptor {

        val newName = Name.identifier("${descriptor.name}\$bound-${lower.callableReferenceCount++}")

        val newDescriptor = SimpleFunctionDescriptorImpl.create(
                containingDeclaration, Annotations.EMPTY,
                newName, CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE)

        val simpleFunctionImplType = simpleFunctionImplClass.defaultType

        // Map each unbound param of original function to parameter of new function.
        val newUnboundParams = unboundParams.mapIndexed { index, param ->
            ValueParameterDescriptorImpl(
                    containingDeclaration = newDescriptor,
                    original = null,
                    index = index + 1,
                    annotations = Annotations.EMPTY,
                    name = param.name,
                    outType = builtIns.nullableAnyType, // Use erased type.
                    declaresDefaultValue = false,
                    isCrossinline = false, isNoinline = false,
                    varargElementType = null,
                    source = SourceElement.NO_SOURCE)
        }

        // `<func>: SimpleFunctionImpl`
        val functionParameter = ValueParameterDescriptorImpl(
                containingDeclaration = newDescriptor,
                original = null,
                index = 0,
                annotations = Annotations.EMPTY,
                name = Name.special("<func>"),
                outType = simpleFunctionImplType,
                declaresDefaultValue = false,
                isCrossinline = false, isNoinline = false,
                varargElementType = null,
                source = SourceElement.NO_SOURCE)

        val newValueParameters = listOf(functionParameter) + newUnboundParams

        val newReturnType = builtIns.nullableAnyType // Use erased type.

        newDescriptor.initialize(null, null, descriptor.typeParameters, newValueParameters,
                newReturnType, Modality.FINAL, Visibilities.PRIVATE)

        return newDescriptor
    }
}
