package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.DeclarationContainerLoweringPass
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.IrCallableReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.singletonList

/**
 * This lowering pass lowers all [IrCallableReference]s to unbound ones.
 */
internal class CallableReferenceLowering(val context: KonanBackendContext) : DeclarationContainerLoweringPass {

    var callableReferenceCount = 0

    override fun lower(irDeclarationContainer: IrDeclarationContainer) {
        irDeclarationContainer.declarations.transformFlat { memberDeclaration ->
            if (memberDeclaration is IrFunction)
                lowerCallableReferences(this, memberDeclaration)
            else
                null
        }
    }
}

/**
 * Replaces all callable references in the function body to unbound ones.
 * Returns the list of this function and all created ones.
 */
private fun lowerCallableReferences(lower: CallableReferenceLowering, function: IrFunction): List<IrFunction> {
    val transformer = CallableReferencesUnbinder(lower, function)
    function.transformChildrenVoid(transformer)
    return function.singletonList() + transformer.createdFunctions
}

private object DECLARATION_ORIGIN_FUNCTION_FOR_CALLABLE_REFERENCE :
        IrDeclarationOriginImpl("FUNCTION_FOR_CALLABLE_REFERENCE")

/**
 * Replaces all callable references in the function body to unbound ones.
 * Adds all created functions to [createdFunctions].
 */
private class CallableReferencesUnbinder(val lower: CallableReferenceLowering,
                                         val function: IrFunction) : IrElementTransformerVoid() {

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

    override fun visitCallableReference(expression: IrCallableReference): IrExpression {
        assert (expression.type.isFunctionType)

        val descriptor = expression.descriptor
        val boundArgs = expression.getArguments()
        val boundParams = boundArgs.map { it.first }
        val unboundParams = descriptor.allValueParameters - boundParams

        val startOffset = expression.startOffset
        val endOffset = expression.endOffset

        // The code below creates a call to `SimpleFunctionImpl` constructor which creates the object implementing
        // the function type.

        // Create the function to be used as `unboundRef` of `SimpleFunctionImpl`:
        val newFunction = createSimpleFunctionImplTarget(descriptor, unboundParams, boundParams, startOffset, endOffset)

        createdFunctions.add(newFunction)

        // Create the call to constructor and its arguments:

        val unboundRef = IrCallableReferenceImpl(startOffset, endOffset,
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
    private fun createSimpleFunctionImplTarget(descriptor: CallableDescriptor,
                                               unboundParams: List<ParameterDescriptor>,
                                               boundParams: List<ParameterDescriptor>,
                                               startOffset: Int, endOffset: Int): IrFunctionImpl {

        // Create new function descriptor:
        val newDescriptor = createSimpleFunctionImplTargetDescriptor(descriptor, unboundParams)

        // Create new function declaration:

        val boundArgsArray = IrCallImpl(startOffset, endOffset, simpleFunctionImplBoundArgsGetter, null).apply {
            dispatchReceiver = IrGetValueImpl(startOffset, endOffset, newDescriptor.valueParameters[0])
        }
        // TODO: do not call boundArgs getter for each bound argument.

        val arrayGet = boundArgsArray.type.memberScope.getContributedDescriptors()
                .filterIsInstance<FunctionDescriptor>().single { it.name.asString() == "get" }

        // TODO: some handling for type parameters is probably required here.

        // Call to old function from new function:
        val call = IrCallImpl(startOffset, endOffset, descriptor)

        // unboundParams are received as all new function parameters except first:
        val newUnboundParams = newDescriptor.valueParameters.drop(1)
        assert (unboundParams.size == newUnboundParams.size)
        call.addArguments(unboundParams.mapIndexed { index, param ->
            param to IrGetValueImpl(startOffset, endOffset, newUnboundParams[index])
        })

        // boundParams are received in `SimpleFunctionImpl.boundArgs`:
        call.addArguments(boundParams.mapIndexed { index, param ->
            param to IrCallImpl(startOffset, endOffset, arrayGet).apply {
                dispatchReceiver = boundArgsArray
                putValueArgument(0, IrConstImpl.int(startOffset, endOffset, builtIns.intType, index))
            }
        })

        val ret = IrReturnImpl(startOffset, endOffset, newDescriptor, call)

        val newFunction = IrFunctionImpl(
                startOffset, endOffset, DECLARATION_ORIGIN_FUNCTION_FOR_CALLABLE_REFERENCE,
                newDescriptor,
                IrBlockBodyImpl(startOffset, endOffset, listOf(ret))
        )

        return newFunction
    }

    /**
     * Creates descriptor for [createSimpleFunctionImplTarget].
     */
    private fun createSimpleFunctionImplTargetDescriptor(descriptor: CallableDescriptor,
                                                         unboundParams: List<ParameterDescriptor>): FunctionDescriptor {

        val newName = Name.identifier("${descriptor.name}\$bound-${lower.callableReferenceCount++}")

        val newDescriptor = SimpleFunctionDescriptorImpl.create(
                function.descriptor.containingDeclaration, Annotations.EMPTY,
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
                    outType = param.type,
                    declaresDefaultValue = false,
                    isCrossinline = false, isNoinline = false,
                    varargElementType = (param as? ValueParameterDescriptor)?.varargElementType,
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

        newDescriptor.initialize(null, null, descriptor.typeParameters, newValueParameters,
                descriptor.returnType, Modality.FINAL, Visibilities.PRIVATE)

        return newDescriptor
    }
}