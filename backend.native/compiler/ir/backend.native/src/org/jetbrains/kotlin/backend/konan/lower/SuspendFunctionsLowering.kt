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
import org.jetbrains.kotlin.backend.common.descriptors.isSuspend
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.getKonanInternalClass
import org.jetbrains.kotlin.backend.konan.descriptors.getKonanInternalFunctions
import org.jetbrains.kotlin.backend.konan.descriptors.isSuspendFunctionInvoke
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.konan.ir.*
import org.jetbrains.kotlin.backend.konan.util.atMostOne
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptorImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.util.createParameterDeclarations
import org.jetbrains.kotlin.ir.util.getArguments
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.replace
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable

internal class SuspendFunctionsLowering(val context: Context): DeclarationContainerLoweringPass {

    private val builtCoroutines = mutableMapOf<FunctionDescriptor, BuiltCoroutine>()
    private val suspendLambdas = mutableMapOf<FunctionDescriptor, IrCallableReference>()

    override fun lower(irDeclarationContainer: IrDeclarationContainer) {
        markSuspendLambdas(irDeclarationContainer)
        irDeclarationContainer.declarations.transformFlat {
            if (it is IrFunction && it.descriptor.isSuspend && it.descriptor.modality != Modality.ABSTRACT)
                transformSuspendFunction(it, suspendLambdas[it.descriptor])
            else null
        }
        transformCallableReferencesToSuspendLambdas(irDeclarationContainer)
        transformCallsToExtensionSuspendFunctions(irDeclarationContainer)
    }

    private fun markSuspendLambdas(irDeclarationContainer: IrDeclarationContainer) {
        irDeclarationContainer.declarations.forEach {
            it.acceptChildrenVoid(object: IrElementVisitorVoid {
                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitCallableReference(expression: IrCallableReference) {
                    expression.acceptChildrenVoid(this)

                    val descriptor = expression.descriptor
                    if (descriptor.isSuspend)
                        suspendLambdas.put(descriptor as FunctionDescriptor, expression)
                }
            })
        }
    }

    private fun transformCallableReferencesToSuspendLambdas(irDeclarationContainer: IrDeclarationContainer) {
        irDeclarationContainer.declarations.forEach {
            it.transformChildrenVoid(object: IrElementTransformerVoid() {

                override fun visitCallableReference(expression: IrCallableReference): IrExpression {
                    expression.transformChildrenVoid(this)

                    val descriptor = expression.descriptor
                    if (!descriptor.isSuspend) return expression
                    val coroutine = builtCoroutines[descriptor]
                            ?: throw Error("Non-local callable reference to suspend lambda: $descriptor")
                    val constructorParameters = coroutine.coroutineConstructorDescriptor.valueParameters
                    val expressionArguments = expression.getArguments().map { it.second }
                    assert (constructorParameters.size == expressionArguments.size,
                            { "Inconsistency between callable reference to suspend lambda and the corresponding coroutine" })
                    val irBuilder = context.createIrBuilder(descriptor, expression.startOffset, expression.endOffset)
                    irBuilder.run {
                        return irCall(coroutine.coroutineConstructorDescriptor).apply {
                            expressionArguments.forEachIndexed { index, argument ->
                                putValueArgument(index, argument) }
                        }
                    }
                }
            })
        }
    }

    private fun transformCallsToExtensionSuspendFunctions(irDeclarationContainer: IrDeclarationContainer) {
        irDeclarationContainer.declarations.forEach {
            it.transformChildrenVoid(object: IrElementTransformerVoid() {

                override fun visitCall(expression: IrCall): IrExpression {
                    expression.transformChildrenVoid(this)

                    val descriptor = expression.descriptor as? FunctionDescriptor
                            ?: return expression

                    if (!descriptor.isSuspendFunctionInvoke || descriptor.extensionReceiverParameter == null)
                        return expression

                    val invokeFunctionDescriptor = descriptor.dispatchReceiverParameter!!.type.memberScope
                            .getContributedFunctions(Name.identifier("invoke"), NoLookupLocation.FROM_BACKEND).single()
                    return IrCallImpl(
                            startOffset = expression.startOffset,
                            endOffset = expression.endOffset,
                            calleeDescriptor = invokeFunctionDescriptor
                    ).apply {
                        dispatchReceiver = expression.dispatchReceiver
                        putValueArgument(0, expression.extensionReceiver)
                        invokeFunctionDescriptor.valueParameters.drop(1).forEach {
                            putValueArgument(it.index, expression.getValueArgument(it.index - 1))
                        }
                    }
                }
            })
        }
    }

    private enum class SuspendFunctionKind {
        NO_SUSPEND_CALLS,
        DELEGATING,
        NEEDS_STATE_MACHINE
    }

    private fun transformSuspendFunction(irFunction: IrFunction, callableReference: IrCallableReference?): List<IrDeclaration>? {
        val suspendFunctionKind = getSuspendFunctionKind(irFunction)
        return when (suspendFunctionKind) {
            SuspendFunctionKind.NO_SUSPEND_CALLS -> {
                removeReturnIfSuspendedCall(irFunction)
                null                                                            // No suspend function calls - just an ordinary function.
            }

            SuspendFunctionKind.DELEGATING -> {                                 // Calls another suspend function at the end.
                removeReturnIfSuspendedCall(irFunction)
                null                                                            // No need in state machine.
            }

            SuspendFunctionKind.NEEDS_STATE_MACHINE -> {
                val coroutine: IrDeclaration = buildCoroutine(irFunction, callableReference)   // Coroutine implementation.
                if (suspendLambdas.contains(irFunction.descriptor))             // Suspend lambdas are called through factory method <create>,
                    listOf(coroutine)                                           // thus we can eliminate original body.
                else
                    listOf(
                            coroutine,
                            irFunction
                    )
            }
        }
    }

    private fun getSuspendFunctionKind(irFunction: IrFunction): SuspendFunctionKind {
        if (suspendLambdas.contains(irFunction.descriptor))
            return SuspendFunctionKind.NEEDS_STATE_MACHINE            // Suspend lambdas always need coroutine implementation.

        val body = irFunction.body
        if (body == null)
            return SuspendFunctionKind.NO_SUSPEND_CALLS

        var numberOfSuspendCalls = 0
        body.acceptVoid(object: IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitCall(expression: IrCall) {
                expression.acceptChildrenVoid(this)

                if (expression.descriptor.isSuspend)
                    ++numberOfSuspendCalls
            }
        })
        // It is important to optimize the case where there is only one suspend call and it is the last statement
        // because we don't need to build a fat coroutine class in that case.
        // This happens a lot in practise because of suspend functions with default arguments.
        // TODO: use TailRecursionCallsCollector.
        val lastStatement = (body as IrBlockBody).statements.lastOrNull()
        val lastCall = when (lastStatement) {
            is IrCall -> lastStatement
            is IrReturn -> {
                var value: IrElement = lastStatement.value
                /*
                 * Check if matches this pattern:
                 * block {
                 *     block {
                 *         .. suspendCall()
                 *     }
                 * }
                 */
                while (value is IrBlock && value.statements.size == 1) {
                    value = value.statements.first()
                }
                value as? IrCall
            }
            else -> null
        }
        val suspendCallAtEnd = lastCall != null && lastCall.descriptor.isSuspend     // Suspend call.
        return when {
            numberOfSuspendCalls == 0   -> SuspendFunctionKind.NO_SUSPEND_CALLS
            numberOfSuspendCalls == 1
                    && suspendCallAtEnd -> SuspendFunctionKind.DELEGATING
            else                        -> SuspendFunctionKind.NEEDS_STATE_MACHINE
        }
    }

    private val getContinuationDescriptor = context.builtIns.getKonanInternalFunctions("getContinuation").single()
    private val returnIfSuspendedDescriptor = context.builtIns.getKonanInternalFunctions("returnIfSuspended").single()

    private fun KotlinType.replace(types: List<KotlinType>) = this.replace(types.map(::TypeProjectionImpl))

    private fun FunctionDescriptor.substitute(vararg types: KotlinType): FunctionDescriptor {
        val typeSubstitutor = TypeSubstitutor.create(
                typeParameters
                        .withIndex()
                        .associateBy({ it.value.typeConstructor }, { TypeProjectionImpl(types[it.index]) })
        )
        return substitute(typeSubstitutor)!!
    }

    private fun removeReturnIfSuspendedCall(irFunction: IrFunction) {
        irFunction.transformChildrenVoid(object: IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildrenVoid(this)

                if (expression.descriptor.original == returnIfSuspendedDescriptor)
                    return expression.getValueArgument(0)!!
                return expression
            }
        })
    }

    private fun buildCoroutine(irFunction: IrFunction, callableReference: IrCallableReference?): IrClass {
        val descriptor = irFunction.descriptor
        val coroutine = CoroutineBuilder(irFunction, callableReference).build()
        builtCoroutines.put(descriptor, coroutine)

        if (callableReference == null) {
            // It is not a lambda - replace original function with a call to constructor of the built coroutine.
            val irBuilder = context.createIrBuilder(descriptor, irFunction.startOffset, irFunction.endOffset)
            irFunction.body = irBuilder.irBlockBody(irFunction) {
                +irReturn(
                        irCall(coroutine.doResumeFunctionDescriptor).apply {
                            dispatchReceiver = irCall(coroutine.coroutineConstructorDescriptor).apply {
                                val functionParameters = irFunction.descriptor.explicitParameters
                                functionParameters.forEachIndexed { index, argument ->
                                    putValueArgument(index, irGet(argument))
                                }
                                putValueArgument(functionParameters.size,
                                        irCall(getContinuationDescriptor.substitute(descriptor.returnType!!)))
                            }
                            putValueArgument(0, irUnit())       // value
                            putValueArgument(1, irNull())       // exception
                        })
            }
        }

        return coroutine.coroutineClass
    }

    private abstract class DescriptorWithIrBuilder<out D: DeclarationDescriptor, out B: IrDeclaration> {

        protected abstract fun buildDescriptor(): D

        protected open fun doInitialize() { }

        protected abstract fun buildIr(): B

        val descriptor by lazy { buildDescriptor() }

        private val builtIr by lazy { buildIr() }
        private var initialized: Boolean = false

        fun initialize() {
            doInitialize()
            initialized = true
        }

        val ir: B
            get() {
                if (!initialized)
                    throw Error("Access to IR before initialization")
                return builtIr
            }
    }

    private class BuiltCoroutine(val coroutineClass: IrClass,
                                 val coroutineConstructorDescriptor: ClassConstructorDescriptor,
                                 val doResumeFunctionDescriptor: FunctionDescriptor)

    private var coroutineId = 0

    private val COROUTINES_FQ_NAME            = FqName.fromSegments(listOf("kotlin", "coroutines", "experimental"))
    private val COROUTINES_INTRINSICS_FQ_NAME = FqName.fromSegments(listOf("kotlin", "coroutines", "experimental", "intrinsics"))
    private val KOTLIN_FQ_NAME                = FqName("kotlin")

    private val coroutinesScope           = context.irModule!!.descriptor.getPackage(COROUTINES_FQ_NAME).memberScope
    private val coroutinesIntrinsicsScope = context.irModule!!.descriptor.getPackage(COROUTINES_INTRINSICS_FQ_NAME).memberScope
    private val kotlinPackageScope        = context.irModule!!.descriptor.getPackage(KOTLIN_FQ_NAME).memberScope

    private val continuationClassDescriptor = coroutinesScope
            .getContributedClassifier(Name.identifier("Continuation"), NoLookupLocation.FROM_BACKEND) as ClassDescriptor
    private val COROUTINE_SUSPENDED = coroutinesIntrinsicsScope
            .getContributedVariables(Name.identifier("COROUTINE_SUSPENDED"), NoLookupLocation.FROM_BACKEND).first()

    private inner class CoroutineBuilder(val irFunction: IrFunction, val callableReference: IrCallableReference?) {

        private val functionParameters = irFunction.descriptor.explicitParameters
        private val boundFunctionParameters = callableReference?.getArguments()?.map { it.first }
        private val unboundFunctionParameters = boundFunctionParameters?.let { functionParameters - it }

        private var tempIndex = 0
        private var suspensionPointIdIndex = 0
        private lateinit var suspendResult: VariableDescriptor
        private lateinit var dataArgument: ValueParameterDescriptor
        private lateinit var exceptionArgument: ValueParameterDescriptor
        private lateinit var coroutineClassDescriptor: ClassDescriptorImpl
        private lateinit var argumentToPropertiesMap: Map<ParameterDescriptor, PropertyDescriptor>
        private val coroutineMembers = mutableListOf<IrDeclaration>()

        private val coroutineImplClassDescriptor = context.builtIns.getKonanInternalClass("CoroutineImpl")
        private val create1FunctionDescriptor = coroutineImplClassDescriptor.unsubstitutedMemberScope
                .getContributedFunctions(Name.identifier("create"), NoLookupLocation.FROM_BACKEND)
                .single { it.valueParameters.size == 1 }
        private val create1CompletionParameter = create1FunctionDescriptor.valueParameters[0]

        fun build(): BuiltCoroutine {
            val superTypes = mutableListOf<KotlinType>(coroutineImplClassDescriptor.defaultType)
            var suspendFunctionClassDescriptor: ClassDescriptor? = null
            var functionClassDescriptor: ClassDescriptor? = null
            if (unboundFunctionParameters != null) {
                // Suspend lambda inherits SuspendFunction.
                val numberOfParameters = unboundFunctionParameters.size
                suspendFunctionClassDescriptor = kotlinPackageScope.getContributedClassifier(
                        Name.identifier("SuspendFunction$numberOfParameters"), NoLookupLocation.FROM_BACKEND) as ClassDescriptor
                superTypes += suspendFunctionClassDescriptor.defaultType.replace(
                        unboundFunctionParameters.map { it.type } + irFunction.descriptor.returnType!!
                )

                functionClassDescriptor = kotlinPackageScope.getContributedClassifier(
                        Name.identifier("Function${numberOfParameters + 1}"), NoLookupLocation.FROM_BACKEND) as ClassDescriptor
                val continuationType = continuationClassDescriptor.defaultType.replace(listOf(irFunction.descriptor.returnType!!))
                superTypes += functionClassDescriptor.defaultType.replace(
                        unboundFunctionParameters.map { it.type }     // Main arguments,
                                + continuationType                    // and continuation.
                                + context.builtIns.nullableAnyType)   // Return type
            }
            coroutineClassDescriptor = ClassDescriptorImpl(
                    /* containingDeclaration = */ irFunction.descriptor.containingDeclaration,
                    /* name                  = */ "${irFunction.descriptor.name}\$${coroutineId++}".synthesizedName,
                    /* modality              = */ Modality.FINAL,
                    /* kind                  = */ ClassKind.CLASS,
                    /* superTypes            = */ superTypes,
                    /* source                = */ SourceElement.NO_SOURCE,
                    /* isExternal            = */ false
            )
            val overriddenMap = mutableMapOf<CallableMemberDescriptor, CallableMemberDescriptor>()
            val constructors = mutableSetOf<ClassConstructorDescriptor>()
            val coroutineConstructorBuilder = createConstructorBuilder()
            constructors.add(coroutineConstructorBuilder.descriptor)

            val doResumeFunctionDescriptor = coroutineImplClassDescriptor.unsubstitutedMemberScope
                    .getContributedFunctions(Name.identifier("doResume"), NoLookupLocation.FROM_BACKEND).single()
            val doResumeMethodBuilder = createDoResumeMethodBuilder(doResumeFunctionDescriptor)
            overriddenMap += doResumeFunctionDescriptor to doResumeMethodBuilder.descriptor

            var coroutineFactoryConstructorBuilder: DescriptorWithIrBuilder<ClassConstructorDescriptor, IrConstructor>? = null
            var createMethodBuilder: DescriptorWithIrBuilder<FunctionDescriptor, IrFunction>? = null
            var invokeMethodBuilder: DescriptorWithIrBuilder<FunctionDescriptor, IrFunction>? = null
            if (callableReference != null) {
                // Suspend lambda - create factory methods.
                coroutineFactoryConstructorBuilder = createFactoryConstructorBuilder(boundFunctionParameters!!)
                constructors.add(coroutineFactoryConstructorBuilder.descriptor)

                val createFunctionDescriptor = coroutineImplClassDescriptor.unsubstitutedMemberScope
                        .getContributedFunctions(Name.identifier("create"), NoLookupLocation.FROM_BACKEND)
                        .atMostOne { it.valueParameters.size == unboundFunctionParameters!!.size + 1 }
                createMethodBuilder = createCreateMethodBuilder(
                        unboundArgs                    = unboundFunctionParameters!!,
                        superFunctionDescriptor        = createFunctionDescriptor,
                        coroutineConstructorDescriptor = coroutineConstructorBuilder.descriptor)
                if (createFunctionDescriptor != null)
                    overriddenMap += createFunctionDescriptor to createMethodBuilder.descriptor

                val invokeFunctionDescriptor = functionClassDescriptor!!.unsubstitutedMemberScope
                        .getContributedFunctions(Name.identifier("invoke"), NoLookupLocation.FROM_BACKEND).single()
                val suspendInvokeFunctionDescriptor = suspendFunctionClassDescriptor!!.unsubstitutedMemberScope
                        .getContributedFunctions(Name.identifier("invoke"), NoLookupLocation.FROM_BACKEND).single()
                invokeMethodBuilder = createInvokeMethodBuilder(
                        suspendFunctionInvokeFunctionDescriptor = suspendInvokeFunctionDescriptor,
                        functionInvokeFunctionDescriptor        = invokeFunctionDescriptor,
                        createFunctionDescriptor                = createMethodBuilder.descriptor,
                        doResumeFunctionDescriptor              = doResumeMethodBuilder.descriptor)
            }

            val inheritedFromCoroutineImpl = coroutineImplClassDescriptor.unsubstitutedMemberScope
                    .getContributedDescriptors()
                    .map { overriddenMap[it] ?: it.createFakeOverrideDescriptor(coroutineClassDescriptor) }
            val contributedDescriptors = (
                    inheritedFromCoroutineImpl + invokeMethodBuilder?.descriptor
                    ).filterNotNull().toList()
            coroutineClassDescriptor.initialize(SimpleMemberScope(contributedDescriptors), constructors, null)

            coroutineConstructorBuilder.initialize()
            coroutineMembers.add(coroutineConstructorBuilder.ir)

            coroutineFactoryConstructorBuilder?.let {
                it.initialize()
                coroutineMembers.add(it.ir)
            }

            createMethodBuilder?.let {
                it.initialize()
                coroutineMembers.add(it.ir)
            }

            invokeMethodBuilder?.let {
                it.initialize()
                coroutineMembers.add(it.ir)
            }

            doResumeMethodBuilder.initialize()
            coroutineMembers.add(doResumeMethodBuilder.ir)

            val coroutineClass = IrClassImpl(
                    startOffset = irFunction.startOffset,
                    endOffset   = irFunction.endOffset,
                    origin      = DECLARATION_ORIGIN_COROUTINE_IMPL,
                    descriptor  = coroutineClassDescriptor,
                    members     = coroutineMembers
            )

            coroutineClass.createParameterDeclarations()

            return BuiltCoroutine(
                    coroutineClass                 = coroutineClass,
                    coroutineConstructorDescriptor = coroutineFactoryConstructorBuilder?.descriptor
                            ?: coroutineConstructorBuilder.descriptor,
                    doResumeFunctionDescriptor     = doResumeMethodBuilder.descriptor)
        }

        private fun createConstructorBuilder()
                = object : DescriptorWithIrBuilder<ClassConstructorDescriptorImpl, IrConstructor>() {

            private val coroutineImplConstructorDescriptor = coroutineImplClassDescriptor.constructors.single()
            private lateinit var constructorParameters: List<ValueParameterDescriptor>

            override fun buildDescriptor(): ClassConstructorDescriptorImpl {
                return ClassConstructorDescriptorImpl.create(
                        /* containingDeclaration = */ coroutineClassDescriptor,
                        /* annotations           = */ Annotations.EMPTY,
                        /* isPrimary             = */ false,
                        /* source                = */ SourceElement.NO_SOURCE
                )
            }

            override fun doInitialize() {
                constructorParameters = (
                        functionParameters
                        + coroutineImplConstructorDescriptor.valueParameters[0] // completion.
                        ).mapIndexed { index, parameter -> parameter.copyAsValueParameter(descriptor, index) }

                descriptor.initialize(constructorParameters, Visibilities.PUBLIC)
                descriptor.returnType = coroutineClassDescriptor.defaultType
            }

            override fun buildIr(): IrConstructor {
                // Save all arguments to fields.
                argumentToPropertiesMap = functionParameters.associate {
                    it to buildPropertyWithBackingField(it.name, it.type, false)
                }

                val startOffset = irFunction.startOffset
                val endOffset = irFunction.endOffset
                val irBuilder = context.createIrBuilder(descriptor, startOffset, endOffset)
                return IrConstructorImpl(
                        startOffset = startOffset,
                        endOffset   = endOffset,
                        origin      = DECLARATION_ORIGIN_COROUTINE_IMPL,
                        descriptor  = descriptor).apply {

                    createParameterDeclarations()

                    body = irBuilder.irBlockBody {
                        val completionParameter = descriptor.valueParameters.last()
                        +IrDelegatingConstructorCallImpl(startOffset, endOffset, coroutineImplConstructorDescriptor).apply {
                            putValueArgument(0, irGet(completionParameter))
                        }
                        +IrInstanceInitializerCallImpl(startOffset, endOffset, coroutineClassDescriptor)
                        functionParameters.forEachIndexed { index, parameter ->
                            +irSetField(irThis(), argumentToPropertiesMap[parameter]!!, irGet(constructorParameters[index]))
                        }
                    }
                }
            }
        }

        private fun ParameterDescriptor.copyAsValueParameter(newOwner: CallableDescriptor, index: Int)
                = when (this) {
            is ValueParameterDescriptor -> this.copy(newOwner, name, index)
            is ReceiverParameterDescriptor -> ValueParameterDescriptorImpl(
                    containingDeclaration = newOwner,
                    original              = null,
                    index                 = index,
                    annotations           = annotations,
                    name                  = name,
                    outType               = type,
                    declaresDefaultValue  = false,
                    isCrossinline         = false,
                    isNoinline            = false,
                    varargElementType     = null,
                    source                = source
            )
            else -> throw Error("Unexpected parameter descriptor: $this")
        }

        private fun createFactoryConstructorBuilder(boundParams: List<ParameterDescriptor>)
                = object : DescriptorWithIrBuilder<ClassConstructorDescriptorImpl, IrConstructor>() {

            private val coroutineImplConstructorDescriptor = coroutineImplClassDescriptor.constructors.single()
            private lateinit var constructorParameters: List<ValueParameterDescriptor>

            override fun buildDescriptor(): ClassConstructorDescriptorImpl {
                return ClassConstructorDescriptorImpl.create(
                        /* containingDeclaration = */ coroutineClassDescriptor,
                        /* annotations           = */ Annotations.EMPTY,
                        /* isPrimary             = */ false,
                        /* source                = */ SourceElement.NO_SOURCE
                )
            }

            override fun doInitialize() {
                constructorParameters = boundParams.mapIndexed { index, parameter ->
                    parameter.copyAsValueParameter(descriptor, index)
                }
                descriptor.initialize(constructorParameters, Visibilities.PUBLIC)
                descriptor.returnType = coroutineClassDescriptor.defaultType
            }

            override fun buildIr(): IrConstructor {
                val startOffset = irFunction.startOffset
                val endOffset = irFunction.endOffset
                val irBuilder = context.createIrBuilder(descriptor, startOffset, endOffset)
                return IrConstructorImpl(
                        startOffset = startOffset,
                        endOffset   = endOffset,
                        origin      = DECLARATION_ORIGIN_COROUTINE_IMPL,
                        descriptor  = descriptor).apply {

                    createParameterDeclarations()

                    body = irBuilder.irBlockBody {
                        +IrDelegatingConstructorCallImpl(startOffset, endOffset, coroutineImplConstructorDescriptor).apply {
                            putValueArgument(0, irNull()) // Completion.
                        }
                        +IrInstanceInitializerCallImpl(startOffset, endOffset, coroutineClassDescriptor)
                        // Save all arguments to fields.
                        boundParams.forEachIndexed { index, parameter ->
                            +irSetField(irThis(), argumentToPropertiesMap[parameter]!!, irGet(constructorParameters[index]))
                        }
                    }
                }
            }
        }

        private fun createCreateMethodBuilder(unboundArgs: List<ParameterDescriptor>,
                                              superFunctionDescriptor: FunctionDescriptor?,
                                              coroutineConstructorDescriptor: ClassConstructorDescriptor)
                = object: DescriptorWithIrBuilder<SimpleFunctionDescriptorImpl, IrFunction>() {

            override fun buildDescriptor() = SimpleFunctionDescriptorImpl.create(
                        /* containingDeclaration = */ coroutineClassDescriptor,
                        /* annotations           = */ Annotations.EMPTY,
                        /* name                  = */ Name.identifier("create"),
                        /* kind                  = */ CallableMemberDescriptor.Kind.DECLARATION,
                        /* source                = */ SourceElement.NO_SOURCE)

            override fun doInitialize() {
                val valueParameters = (
                        unboundArgs + create1CompletionParameter
                        ).mapIndexed { index, parameter ->
                    parameter.copyAsValueParameter(descriptor, index)
                }

                descriptor.initialize(
                        /* receiverParameterType        = */ null,
                        /* dispatchReceiverParameter    = */ coroutineClassDescriptor.thisAsReceiverParameter,
                        /* typeParameters               = */ emptyList(),
                        /* unsubstitutedValueParameters = */ valueParameters,
                        /* unsubstitutedReturnType      = */ coroutineClassDescriptor.defaultType,
                        /* modality                     = */ Modality.FINAL,
                        /* visibility                   = */ Visibilities.PRIVATE).apply {
                    if (superFunctionDescriptor != null) {
                        overriddenDescriptors           +=   superFunctionDescriptor.overriddenDescriptors
                        overriddenDescriptors           +=   superFunctionDescriptor
                    }
                }
            }

            override fun buildIr(): IrFunction {
                val startOffset = irFunction.startOffset
                val endOffset = irFunction.endOffset
                val ourDescriptor = descriptor
                val irBuilder = context.createIrBuilder(descriptor, startOffset, endOffset)
                return IrFunctionImpl(
                        startOffset = startOffset,
                        endOffset   = endOffset,
                        origin      = DECLARATION_ORIGIN_COROUTINE_IMPL,
                        descriptor  = descriptor).apply {

                    createParameterDeclarations()

                    body = irBuilder.irBlockBody(startOffset, endOffset) {
                        +irReturn(
                                irCall(coroutineConstructorDescriptor).apply {
                                    var unboundIndex = 0
                                    val unboundArgsSet = unboundArgs.toSet()
                                    functionParameters.map {
                                        if (unboundArgsSet.contains(it))
                                            irGet(ourDescriptor.valueParameters[unboundIndex++])
                                        else
                                            irGet(irThis(), argumentToPropertiesMap[it]!!)
                                    }.forEachIndexed { index, argument ->
                                        putValueArgument(index, argument)
                                    }
                                    putValueArgument(functionParameters.size, irGet(ourDescriptor.valueParameters[unboundIndex]))
                                    assert(unboundIndex == ourDescriptor.valueParameters.size - 1,
                                            { "Not all arguments of <create> are used" })
                                })
                    }
                }
            }
        }

        private fun createInvokeMethodBuilder(suspendFunctionInvokeFunctionDescriptor: FunctionDescriptor,
                                              functionInvokeFunctionDescriptor: FunctionDescriptor,
                                              createFunctionDescriptor: FunctionDescriptor,
                                              doResumeFunctionDescriptor: FunctionDescriptor)
                = object: DescriptorWithIrBuilder<SimpleFunctionDescriptorImpl, IrFunction>() {

            override fun buildDescriptor() = SimpleFunctionDescriptorImpl.create(
                        /* containingDeclaration = */ coroutineClassDescriptor,
                        /* annotations           = */ Annotations.EMPTY,
                        /* name                  = */ Name.identifier("invoke"),
                        /* kind                  = */ CallableMemberDescriptor.Kind.DECLARATION,
                        /* source                = */ SourceElement.NO_SOURCE)

            override fun doInitialize() {
                val valueParameters = createFunctionDescriptor.valueParameters
                        // Skip completion - invoke() already has it implicitly as a suspend function.
                        .take(createFunctionDescriptor.valueParameters.size - 1)
                        .map { it.copyAsValueParameter(this.descriptor, it.index) }

                this.descriptor.initialize(
                        /* receiverParameterType        = */ null,
                        /* dispatchReceiverParameter    = */ coroutineClassDescriptor.thisAsReceiverParameter,
                        /* typeParameters               = */ emptyList(),
                        /* unsubstitutedValueParameters = */ valueParameters,
                        /* unsubstitutedReturnType      = */ irFunction.descriptor.returnType,
                        /* modality                     = */ Modality.FINAL,
                        /* visibility                   = */ Visibilities.PRIVATE).apply {
                    overriddenDescriptors               +=   suspendFunctionInvokeFunctionDescriptor
                    overriddenDescriptors               +=   functionInvokeFunctionDescriptor
                    isSuspend                           =    true
                }
            }

            override fun buildIr(): IrFunction {
                val startOffset = irFunction.startOffset
                val endOffset = irFunction.endOffset
                val ourDescriptor = this.descriptor
                val irBuilder = context.createIrBuilder(this.descriptor, startOffset, endOffset)
                return IrFunctionImpl(
                        startOffset = startOffset,
                        endOffset   = endOffset,
                        origin      = DECLARATION_ORIGIN_COROUTINE_IMPL,
                        descriptor  = this.descriptor).apply {

                    createParameterDeclarations()

                    body = irBuilder.irBlockBody(startOffset, endOffset) {
                        +irReturn(
                                irCall(doResumeFunctionDescriptor).apply {
                                    dispatchReceiver = irCall(createFunctionDescriptor).apply {
                                        dispatchReceiver = irThis()
                                        ourDescriptor.valueParameters.forEach {
                                            putValueArgument(it.index, irGet(it))
                                        }
                                        putValueArgument(ourDescriptor.valueParameters.size,
                                                irCall(getContinuationDescriptor.substitute(ourDescriptor.returnType!!)))
                                    }
                                    putValueArgument(0, irUnit())       // value
                                    putValueArgument(1, irNull())       // exception
                                }
                        )
                    }
                }
            }
        }

        private fun createPropertyGetterBuilder(propertyDescriptor: PropertyDescriptor, type: KotlinType)
                = object: DescriptorWithIrBuilder<PropertyGetterDescriptorImpl, IrFunction>() {

            override fun buildDescriptor() = PropertyGetterDescriptorImpl(
                    /* correspondingProperty = */ propertyDescriptor,
                    /* annotations           = */ Annotations.EMPTY,
                    /* modality              = */ Modality.FINAL,
                    /* visibility            = */ Visibilities.PRIVATE,
                    /* isDefault             = */ false,
                    /* isExternal            = */ false,
                    /* isInline              = */ false,
                    /* kind                  = */ CallableMemberDescriptor.Kind.DECLARATION,
                    /* original              = */ null,
                    /* source                = */ SourceElement.NO_SOURCE
            )

            override fun doInitialize() {
                descriptor.apply {
                    initialize(type)
                }
            }

            override fun buildIr() = IrFunctionImpl(
                    startOffset = irFunction.startOffset,
                    endOffset   = irFunction.endOffset,
                    origin      = DECLARATION_ORIGIN_COROUTINE_IMPL,
                    descriptor  = descriptor).apply {

                createParameterDeclarations()

                body = context.createIrBuilder(descriptor, startOffset, endOffset).irBlockBody {
                    +irReturn(irGetField(irThis(), propertyDescriptor))
                }
            }
        }

        private fun createPropertySetterBuilder(propertyDescriptor: PropertyDescriptor, type: KotlinType)
                = object: DescriptorWithIrBuilder<PropertySetterDescriptorImpl, IrFunction>() {

            override fun buildDescriptor() = PropertySetterDescriptorImpl(
                    /* correspondingProperty = */ propertyDescriptor,
                    /* annotations           = */ Annotations.EMPTY,
                    /* modality              = */ Modality.FINAL,
                    /* visibility            = */ Visibilities.PRIVATE,
                    /* isDefault             = */ false,
                    /* isExternal            = */ false,
                    /* isInline              = */ false,
                    /* kind                  = */ CallableMemberDescriptor.Kind.DECLARATION,
                    /* original              = */ null,
                    /* source                = */ SourceElement.NO_SOURCE
            )

            lateinit var valueParameterDescriptor: ValueParameterDescriptor

            override fun doInitialize() {
                descriptor.apply {
                    valueParameterDescriptor = ValueParameterDescriptorImpl(
                            containingDeclaration = this,
                            original              = null,
                            index                 = 0,
                            annotations           = Annotations.EMPTY,
                            name                  = Name.identifier("value"),
                            outType               = type,
                            declaresDefaultValue  = false,
                            isCrossinline         = false,
                            isNoinline            = false,
                            varargElementType     = null,
                            source                = SourceElement.NO_SOURCE
                    )

                    initialize(valueParameterDescriptor)
                }
            }

            override fun buildIr() = IrFunctionImpl(
                    startOffset = irFunction.startOffset,
                    endOffset   = irFunction.endOffset,
                    origin      = DECLARATION_ORIGIN_COROUTINE_IMPL,
                    descriptor  = descriptor).apply {

                createParameterDeclarations()

                body = context.createIrBuilder(descriptor, startOffset, endOffset).irBlockBody {
                    +irSetField(irThis(), propertyDescriptor, irGet(valueParameterDescriptor))
                }
            }
        }

        private fun createPropertyWithBackingFieldBuilder(name: Name, type: KotlinType, isMutable: Boolean)
                = object: DescriptorWithIrBuilder<PropertyDescriptorImpl, IrProperty>() {

            private lateinit var getterBuilder: DescriptorWithIrBuilder<PropertyGetterDescriptorImpl, IrFunction>
            private var setterBuilder: DescriptorWithIrBuilder<PropertySetterDescriptorImpl, IrFunction>? = null

            override fun buildDescriptor() = PropertyDescriptorImpl.create(
                    /* containingDeclaration = */ coroutineClassDescriptor,
                    /* annotations           = */ Annotations.EMPTY,
                    /* modality              = */ Modality.FINAL,
                    /* visibility            = */ Visibilities.PRIVATE,
                    /* isVar                 = */ isMutable,
                    /* name                  = */ name,
                    /* kind                  = */ CallableMemberDescriptor.Kind.DECLARATION,
                    /* source                = */ SourceElement.NO_SOURCE,
                    /* lateInit              = */ false,
                    /* isConst               = */ false,
                    /* isHeader              = */ false,
                    /* isImpl                = */ false,
                    /* isExternal            = */ false,
                    /* isDelegated           = */ false)

            override fun doInitialize() {
                getterBuilder = createPropertyGetterBuilder(descriptor, type).apply { initialize() }
                if (isMutable)
                    setterBuilder = createPropertySetterBuilder(descriptor, type).apply { initialize() }
                descriptor.initialize(getterBuilder.descriptor, setterBuilder?.descriptor)
                val receiverType: KotlinType? = null
                descriptor.setType(type, emptyList(), coroutineClassDescriptor.thisAsReceiverParameter, receiverType)
            }

            override fun buildIr(): IrProperty {
                val startOffset = irFunction.startOffset
                val endOffset = irFunction.endOffset
                val backingField = IrFieldImpl(
                        startOffset = startOffset,
                        endOffset   = endOffset,
                        origin      = DECLARATION_ORIGIN_COROUTINE_IMPL,
                        descriptor  = descriptor)
                return IrPropertyImpl(
                        startOffset  = startOffset,
                        endOffset    = endOffset,
                        origin       = DECLARATION_ORIGIN_COROUTINE_IMPL,
                        isDelegated  = false,
                        descriptor   = descriptor,
                        backingField = backingField,
                        getter       = getterBuilder.ir,
                        setter       = setterBuilder?.ir)
            }
        }

        private fun buildPropertyWithBackingField(name: Name, type: KotlinType, isMutable: Boolean): PropertyDescriptor {
            val propertyBuilder = createPropertyWithBackingFieldBuilder(name, type, isMutable).apply { initialize() }

            coroutineMembers.add(propertyBuilder.ir)
            return propertyBuilder.descriptor
        }

        private fun createDoResumeMethodBuilder(doResumeFunctionDescriptor: FunctionDescriptor)
                = object: DescriptorWithIrBuilder<FunctionDescriptor, IrFunction>() {

            override fun buildDescriptor() = doResumeFunctionDescriptor.createOverriddenDescriptor(coroutineClassDescriptor)

            override fun doInitialize() { }

            override fun buildIr(): IrFunction {
                dataArgument = descriptor.valueParameters[0]
                exceptionArgument = descriptor.valueParameters[1]
                suspendResult = IrTemporaryVariableDescriptorImpl(
                        containingDeclaration = irFunction.descriptor,
                        name                  = "suspendResult".synthesizedName,
                        outType               = context.builtIns.nullableAnyType,
                        isMutable             = true)
                val label = coroutineClassDescriptor.unsubstitutedMemberScope
                        .getContributedVariables(Name.identifier("label"), NoLookupLocation.FROM_BACKEND).single()

                val originalBody = irFunction.body!!
                val startOffset = irFunction.startOffset
                val endOffset = irFunction.endOffset
                val irBuilder = context.createIrBuilder(descriptor, startOffset, endOffset)
                return IrFunctionImpl(
                        startOffset = startOffset,
                        endOffset   = endOffset,
                        origin      = DECLARATION_ORIGIN_COROUTINE_IMPL,
                        descriptor  = descriptor).apply {

                    createParameterDeclarations()

                    body = irBuilder.irBlockBody(startOffset, endOffset) {

                        // Extract all suspend calls to temporaries in order to make correct jumps to them.
                        originalBody.transformChildrenVoid(ExpressionSlicer(label.type))

                        val liveLocals = computeLivenessAtSuspensionPoints(originalBody)

                        val immutableLiveLocals = liveLocals.values.flatten().filterNot { it.isVar }.toSet()
                        val localsMap = immutableLiveLocals.associate {
                            it to IrTemporaryVariableDescriptorImpl(
                                    containingDeclaration = irFunction.descriptor,
                                    name                  = it.name,
                                    outType               = it.type,
                                    isMutable             = true)
                        }

                        if (localsMap.isNotEmpty())
                            transformVariables(originalBody, localsMap)    // Make variables mutable in order to save/restore them.

                        val localToPropertyMap = mutableMapOf<VariableDescriptor, PropertyDescriptor>()
                        // TODO: optimize by using the same property for different locals.
                        liveLocals.values.forEach { scope ->
                            scope.forEach {
                                localToPropertyMap.getOrPut(it) {
                                    buildPropertyWithBackingField(it.name, it.type, true)
                                }
                            }
                        }

                        originalBody.transformChildrenVoid(object : IrElementTransformerVoid() {

                            // Replace returns to refer to the new function.
                            override fun visitReturn(expression: IrReturn): IrExpression {
                                expression.transformChildrenVoid(this)

                                return if (expression.returnTarget != irFunction.descriptor)
                                           expression
                                       else
                                           irReturn(expression.value)
                            }

                            // Replace function arguments loading with properties reading.
                            override fun visitGetValue(expression: IrGetValue): IrExpression {
                                expression.transformChildrenVoid(this)

                                val capturedValue = argumentToPropertiesMap[expression.descriptor]
                                        ?: return expression
                                return irGet(irThis(), capturedValue)
                            }

                            // Save/restore state at suspension points.
                            override fun visitExpression(expression: IrExpression): IrExpression {
                                expression.transformChildrenVoid(this)

                                val suspensionPoint = expression as? IrSuspensionPoint
                                        ?: return expression

                                suspensionPoint.transformChildrenVoid(object : IrElementTransformerVoid() {
                                    override fun visitCall(expression: IrCall): IrExpression {
                                        expression.transformChildrenVoid(this)

                                        val descriptor = expression.descriptor
                                        when (descriptor) {
                                            saveStateDescriptor -> {
                                                val scope = liveLocals[suspensionPoint]!!
                                                return irBlock(expression) {
                                                    scope.forEach {
                                                        +irSet(irThis(), localToPropertyMap[it]!!, irGet(localsMap[it] ?: it))
                                                    }
                                                    +irSet(irThis(), label, irGet(suspensionPoint.suspensionPointIdParameter.descriptor))
                                                }
                                            }
                                            restoreStateDescriptor -> {
                                                val scope = liveLocals[suspensionPoint]!!
                                                return irBlock(expression) {
                                                    scope.forEach {
                                                        +irSetVar(localsMap[it] ?: it, irGet(irThis(), localToPropertyMap[it]!!))
                                                    }
                                                }
                                            }
                                        }
                                        return expression
                                    }
                                })

                                return suspensionPoint
                            }
                        })
                        val statements = (originalBody as IrBlockBody).statements
                        +irVar(suspendResult, null)
                        +IrSuspendableExpressionImpl(
                                startOffset       = startOffset,
                                endOffset         = endOffset,
                                type              = context.builtIns.unitType,
                                suspensionPointId = irGet(irThis(), label),
                                result            = irBlock(startOffset, endOffset) {
                                    +irThrowIfNotNull(exceptionArgument)    // Coroutine might start with an exception.
                                    statements.forEach { +it }
                                })
                        if (irFunction.descriptor.returnType!!.isUnit())
                            +irReturn(irUnit())                             // Insert explicit return for Unit functions.
                    }
                }
            }
        }

        private fun transformVariables(element: IrElement, variablesMap: Map<VariableDescriptor, VariableDescriptor>) {
            element.transformChildrenVoid(object: IrElementTransformerVoid() {

                override fun visitGetValue(expression: IrGetValue): IrExpression {
                    expression.transformChildrenVoid(this)

                    val newVariable = variablesMap[expression.descriptor]
                            ?: return expression

                    return IrGetValueImpl(
                            startOffset = expression.startOffset,
                            endOffset   = expression.endOffset,
                            descriptor  = newVariable,
                            origin      = expression.origin)
                }

                override fun visitSetVariable(expression: IrSetVariable): IrExpression {
                    expression.transformChildrenVoid(this)

                    val newVariable = variablesMap[expression.descriptor]
                            ?: return expression

                    return IrSetVariableImpl(
                            startOffset = expression.startOffset,
                            endOffset   = expression.endOffset,
                            descriptor  = newVariable,
                            value       = expression.value,
                            origin      = expression.origin)
                }

                override fun visitVariable(declaration: IrVariable): IrStatement {
                    declaration.transformChildrenVoid(this)

                    val newVariable = variablesMap[declaration.descriptor]
                            ?: return declaration

                    return IrVariableImpl(
                            startOffset = declaration.startOffset,
                            endOffset   = declaration.endOffset,
                            origin      = declaration.origin,
                            descriptor  = newVariable,
                            initializer = declaration.initializer)
                }
            })
        }

        private fun computeLivenessAtSuspensionPoints(body: IrBody): Map<IrSuspensionPoint, List<VariableDescriptor>> {
            // TODO: data flow analysis.
            // Just save all visible for now.
            val result = mutableMapOf<IrSuspensionPoint, List<VariableDescriptor>>()
            body.acceptChildrenVoid(object: VariablesScopeTracker() {

                override fun visitExpression(expression: IrExpression) {
                    val suspensionPoint = expression as? IrSuspensionPoint
                    if (suspensionPoint == null) {
                        super.visitExpression(expression)
                        return
                    }

                    suspensionPoint.result.acceptChildrenVoid(this)
                    suspensionPoint.resumeResult.acceptChildrenVoid(this)

                    val visibleVariables = mutableListOf<VariableDescriptor>()
                    scopeStack.forEach { visibleVariables += it }
                    result.put(suspensionPoint, visibleVariables)
                }
            })

            return result
        }

        // These are marker descriptors to split up the lowering on two parts.
        private val saveStateDescriptor = SimpleFunctionDescriptorImpl.create(
                irFunction.descriptor,
                Annotations.EMPTY,
                "saveState".synthesizedName,
                CallableMemberDescriptor.Kind.SYNTHESIZED,
                SourceElement.NO_SOURCE).apply {
            initialize(null, null, emptyList(), emptyList(), context.builtIns.unitType, Modality.ABSTRACT, Visibilities.PRIVATE)
        }

        private val restoreStateDescriptor = SimpleFunctionDescriptorImpl.create(
                irFunction.descriptor,
                Annotations.EMPTY,
                "restoreState".synthesizedName,
                CallableMemberDescriptor.Kind.SYNTHESIZED,
                SourceElement.NO_SOURCE).apply {
            initialize(null, null, emptyList(), emptyList(), context.builtIns.unitType, Modality.ABSTRACT, Visibilities.PRIVATE)
        }

        private inner class ExpressionSlicer(val suspensionPointIdType: KotlinType): IrElementTransformerVoid() {
            // TODO: optimize - it has square complexity.

            override fun visitSetField(expression: IrSetField): IrExpression {
                expression.transformChildrenVoid(this)

                return sliceExpression(expression)
            }

            override fun visitMemberAccess(expression: IrMemberAccessExpression): IrExpression {
                expression.transformChildrenVoid(this)

                return sliceExpression(expression)
            }

            private fun sliceExpression(expression: IrExpression): IrExpression {
                val irBuilder = context.createIrBuilder(irFunction.descriptor, expression.startOffset, expression.endOffset)
                irBuilder.run {
                    val children = when (expression) {
                        is IrSetField -> listOf(expression.receiver, expression.value)
                        is IrMemberAccessExpression -> (
                                listOf(expression.dispatchReceiver, expression.extensionReceiver)
                                        + expression.descriptor.valueParameters.map { expression.getValueArgument(it.index) }
                                )
                        else -> throw Error("Unexpected expression: $expression")
                    }

                    val numberOfChildren = children.size

                    val hasSuspendCallInTail = BooleanArray(numberOfChildren + 1)
                    for (i in numberOfChildren - 1 downTo 0)
                        hasSuspendCallInTail[i] = hasSuspendCallInTail[i + 1] || children[i].let { it != null && it.hasSuspendCalls() }

                    val newChildren = arrayOfNulls<IrExpression?>(numberOfChildren)
                    val tempStatements = mutableListOf<IrStatement>()
                    var first = true
                    for ((index, child) in children.withIndex()) {
                        if (child == null) continue
                        val transformedChild =
                                if (!child.isSpecialBlock())
                                    child
                                else {
                                    val statements = (child as IrBlock).statements
                                    tempStatements += statements.take(statements.size - 1)
                                    statements.last() as IrExpression
                                }
                        if (first && !hasSuspendCallInTail[index + 1]) {
                            // Don't extract suspend call to a temporary if it is the first argument and is the only suspend call.
                            newChildren[index] = transformedChild
                            first = false
                            continue
                        }
                        first = false
                        if (transformedChild.isPure() || !hasSuspendCallInTail[index])
                            newChildren[index] = transformedChild
                        else {
                            // Save to temporary in order to save execution order.
                            val tmp = IrTemporaryVariableDescriptorImpl(
                                    containingDeclaration = irFunction.descriptor,
                                    name                  = "tmp${tempIndex++}".synthesizedName,
                                    outType               = transformedChild.type)
                            tempStatements += irVar(tmp, transformedChild)
                            newChildren[index] = irGet(tmp)
                        }
                    }

                    var calledSaveState = false
                    if (expression.isSuspendCall) {
                        val lastChild = newChildren.last()
                        if (lastChild != null) {
                            // Save state as late as possible.
                            calledSaveState = true
                            newChildren[numberOfChildren - 1] =
                                    irBlock(lastChild) {
                                        if (lastChild.isPure()) {
                                            +irCall(saveStateDescriptor)
                                            +lastChild
                                        } else {
                                            val tmp = IrTemporaryVariableDescriptorImpl(
                                                    containingDeclaration = irFunction.descriptor,
                                                    name                  = "tmp${tempIndex++}".synthesizedName,
                                                    outType               = lastChild.type)
                                            +irVar(tmp, lastChild)
                                            +irCall(saveStateDescriptor)
                                            +irGet(tmp)
                                        }
                                    }
                        }
                    }

                    when (expression) {
                        is IrSetField -> {
                            expression.receiver = newChildren[0]
                            expression.value = newChildren[1]!!
                        }
                        is IrMemberAccessExpression -> {
                            expression.dispatchReceiver = newChildren[0]
                            expression.extensionReceiver = newChildren[1]
                            newChildren.drop(2).forEachIndexed { index, newChild ->
                                expression.putValueArgument(index, newChild)
                            }
                        }
                    }

                    if (!expression.isSuspendCall && !expression.isReturnIfSuspendedCall)
                        return irWrap(expression, tempStatements)

                    val suspendCall = if (expression.isReturnIfSuspendedCall)
                                          (expression as IrCall).getValueArgument(0)!!
                                      else expression
                    val suspensionPointIdParameter = IrTemporaryVariableDescriptorImpl(
                            containingDeclaration = irFunction.descriptor,
                            name                  = "suspensionPointId${suspensionPointIdIndex++}".synthesizedName,
                            outType               = suspensionPointIdType)
                    val suspensionPoint = IrSuspensionPointImpl(
                            startOffset                = startOffset,
                            endOffset                  = endOffset,
                            type                       = context.builtIns.nullableAnyType,
                            suspensionPointIdParameter = irVar(suspensionPointIdParameter, null),
                            result                     = irBlock(startOffset, endOffset) {
                                if (!calledSaveState)
                                    +irCall(saveStateDescriptor)
                                +irSetVar(suspendResult, suspendCall)
                                +irReturnIfSuspended(suspendResult)
                                +irGet(suspendResult)
                            },
                            resumeResult               = irBlock(startOffset, endOffset) {
                                +irCall(restoreStateDescriptor)
                                +irThrowIfNotNull(exceptionArgument)
                                +irGet(dataArgument)
                            })
                    val expressionResult = when {
                        suspendCall.type.isUnit() -> irImplicitCoercionToUnit(suspensionPoint)
                        else -> irCast(suspensionPoint, suspendCall.type, suspendCall.type)
                    }
                    return irBlock(expression) {
                        tempStatements.forEach { +it }
                        +expressionResult
                    }
                }

            }

            private fun IrBuilderWithScope.irWrap(expression: IrExpression, tempStatements: List<IrStatement>)
                    = if (tempStatements.isEmpty())
                          expression
                      else irBlock(expression, STATEMENT_ORIGIN_COROUTINE_IMPL) {
                          tempStatements.forEach { +it }
                          +expression
                      }

            private val IrExpression.isSuspendCall: Boolean
                get() = this is IrCall && this.descriptor.isSuspend

            private object STATEMENT_ORIGIN_COROUTINE_IMPL :
                    IrStatementOriginImpl("COROUTINE_IMPL")

            private fun IrElement.isSpecialBlock()
                    = this is IrBlock && this.origin == STATEMENT_ORIGIN_COROUTINE_IMPL

            private fun IrElement.hasSuspendCalls(): Boolean {
                var hasSuspendCalls = false
                acceptVoid(object : IrElementVisitorVoid {
                    override fun visitElement(element: IrElement) {
                        element.acceptChildrenVoid(this)
                    }

                    override fun visitCall(expression: IrCall) {
                        expression.acceptChildrenVoid(this)
                        hasSuspendCalls = hasSuspendCalls || expression.isSuspendCall
                    }
                })

                return hasSuspendCalls
            }

            private fun IrExpression.isPure(): Boolean {
                return when (this) {
                    is IrConst<*> -> true
                    is IrCall -> false // TODO: skip builtin operators.
                    is IrTypeOperatorCall -> this.argument.isPure() && this.operator != IrTypeOperator.CAST
                    is IrGetValue -> !this.descriptor.let { it is VariableDescriptor && it.isVar }
                    else -> false
                }
            }

            private val IrExpression.isReturnIfSuspendedCall: Boolean
                get() = this is IrCall && this.descriptor.original == returnIfSuspendedDescriptor
        }

        private object DECLARATION_ORIGIN_COROUTINE_IMPL :
                IrDeclarationOriginImpl("COROUTINE_IMPL")

        private fun IrBuilderWithScope.irVar(descriptor: VariableDescriptor, initializer: IrExpression?) =
                IrVariableImpl(startOffset, endOffset, DECLARATION_ORIGIN_COROUTINE_IMPL, descriptor, initializer)

        private fun IrBuilderWithScope.irReturnIfSuspended(value: ValueDescriptor) =
                irIfThen(irEqeqeq(irGet(value), irGet(COROUTINE_SUSPENDED)),
                        irReturn(irGet(value)))

        private fun IrBuilderWithScope.irThrowIfNotNull(exception: ValueDescriptor) =
                irIfThen(irNot(irEqeqeq(irGet(exception), irNull())),
                        irThrow(irImplicitCast(irGet(exception), exception.type.makeNotNullable())))
    }

    private open class VariablesScopeTracker: IrElementVisitorVoid {

        protected val scopeStack = mutableListOf<MutableSet<VariableDescriptor>>(mutableSetOf())

        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitContainerExpression(expression: IrContainerExpression) {
            if (!expression.isTransparentScope)
                scopeStack.push(mutableSetOf())
            super.visitContainerExpression(expression)
            if (!expression.isTransparentScope)
                scopeStack.pop()
        }

        override fun visitCatch(aCatch: IrCatch) {
            scopeStack.push(mutableSetOf(aCatch.parameter))
            super.visitCatch(aCatch)
            scopeStack.pop()
        }

        override fun visitVariable(declaration: IrVariable) {
            super.visitVariable(declaration)
            scopeStack.peek()!!.add(declaration.descriptor)
        }
    }
}