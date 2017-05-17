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
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.util.createParameterDeclarations
import org.jetbrains.kotlin.ir.util.getArguments
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.backend.common.descriptors.*

internal class SuspendFunctionsLowering(val context: Context): DeclarationContainerLoweringPass {

    private val builtCoroutines = mutableMapOf<FunctionDescriptor, BuiltCoroutine>()
    private val suspendLambdas = mutableMapOf<FunctionDescriptor, IrFunctionReference>()

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

                override fun visitFunctionReference(expression: IrFunctionReference) {
                    expression.acceptChildrenVoid(this)

                    val descriptor = expression.descriptor
                    if (descriptor.isSuspend)
                        suspendLambdas.put(descriptor, expression)
                }
            })
        }
    }

    private fun transformCallableReferencesToSuspendLambdas(irDeclarationContainer: IrDeclarationContainer) {
        irDeclarationContainer.declarations.forEach {
            it.transformChildrenVoid(object: IrElementTransformerVoid() {

                override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
                    expression.transformChildrenVoid(this)

                    val descriptor = expression.descriptor
                    if (!descriptor.isSuspend)
                        return expression
                    val coroutine = builtCoroutines[descriptor]
                            ?: throw Error("Non-local callable reference to suspend lambda: $descriptor")
                    val constructorParameters = coroutine.coroutineConstructor.valueParameters
                    val expressionArguments = expression.getArguments().map { it.second }
                    assert (constructorParameters.size == expressionArguments.size,
                            { "Inconsistency between callable reference to suspend lambda and the corresponding coroutine" })
                    val irBuilder = context.createIrBuilder(expression.symbol, expression.startOffset, expression.endOffset)
                    irBuilder.run {
                        return irCall(coroutine.coroutineConstructor.symbol).apply {
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

                    val descriptor = expression.descriptor

                    if (!descriptor.isSuspendFunctionInvoke || descriptor.extensionReceiverParameter == null)
                        return expression

                    val invokeFunctionDescriptor = descriptor.dispatchReceiverParameter!!.type.memberScope
                            .getContributedFunctions(Name.identifier("invoke"), NoLookupLocation.FROM_BACKEND).single()
                    return IrCallImpl(
                            startOffset      = expression.startOffset,
                            endOffset        = expression.endOffset,
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

    private fun transformSuspendFunction(irFunction: IrFunction, functionReference: IrFunctionReference?): List<IrDeclaration>? {
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
                val coroutine = buildCoroutine(irFunction, functionReference)   // Coroutine implementation.
                if (suspendLambdas.contains(irFunction.descriptor))             // Suspend lambdas are called through factory method <create>,
                    listOf(coroutine)                                           // thus we can eliminate original body.
                else
                    listOf<IrDeclaration>(
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
                ?: return SuspendFunctionKind.NO_SUSPEND_CALLS

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

    private fun buildCoroutine(irFunction: IrFunction, functionReference: IrFunctionReference?): IrClass {
        val descriptor = irFunction.descriptor
        val coroutine = CoroutineBuilder(irFunction, functionReference).build()
        builtCoroutines.put(descriptor, coroutine)

        if (functionReference == null) {
            // It is not a lambda - replace original function with a call to constructor of the built coroutine.
            val irBuilder = context.createIrBuilder(irFunction.symbol, irFunction.startOffset, irFunction.endOffset)
            irFunction.body = irBuilder.irBlockBody(irFunction) {
                +irReturn(
                        irCall(coroutine.doResumeFunction.symbol).apply {
                            dispatchReceiver = irCall(coroutine.coroutineConstructor.symbol).apply {
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

    private class BuiltCoroutine(val coroutineClass: IrClass,
                                 val coroutineConstructor: IrConstructor,
                                 val doResumeFunction: IrFunction)

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

    private inner class CoroutineBuilder(val irFunction: IrFunction, val functionReference: IrFunctionReference?) {

        private val functionParameters = irFunction.descriptor.explicitParameters
        private val boundFunctionParameters = functionReference?.getArguments()?.map { it.first }
        private val unboundFunctionParameters = boundFunctionParameters?.let { functionParameters - it }

        private var tempIndex = 0
        private var suspensionPointIdIndex = 0
        private lateinit var suspendResult: IrVariableSymbol
        private lateinit var dataArgument: IrValueParameterSymbol
        private lateinit var exceptionArgument: IrValueParameterSymbol
        private lateinit var coroutineClassDescriptor: ClassDescriptorImpl
        private lateinit var coroutineClass: IrClassImpl
        private lateinit var coroutineClassThis: IrValueParameterSymbol
        private lateinit var argumentToPropertiesMap: Map<ParameterDescriptor, IrFieldSymbol>

        private val coroutineImplClassDescriptor = context.builtIns.getKonanInternalClass("CoroutineImpl")
        private val create1FunctionDescriptor = coroutineImplClassDescriptor.unsubstitutedMemberScope
                .getContributedFunctions(Name.identifier("create"), NoLookupLocation.FROM_BACKEND)
                .single { it.valueParameters.size == 1 }
        private val create1CompletionParameter = create1FunctionDescriptor.valueParameters[0]

        fun build(): BuiltCoroutine {
            val superTypes = mutableListOf<KotlinType>(coroutineImplClassDescriptor.defaultType)
            var suspendFunctionClassDescriptor: ClassDescriptor? = null
            var functionClassDescriptor: ClassDescriptor? = null
            var suspendFunctionClassTypeArguments: List<KotlinType>? = null
            var functionClassTypeArguments: List<KotlinType>? = null
            if (unboundFunctionParameters != null) {
                // Suspend lambda inherits SuspendFunction.
                val numberOfParameters = unboundFunctionParameters.size
                suspendFunctionClassDescriptor = kotlinPackageScope.getContributedClassifier(
                        Name.identifier("SuspendFunction$numberOfParameters"), NoLookupLocation.FROM_BACKEND) as ClassDescriptor
                val unboundParameterTypes = unboundFunctionParameters.map { it.type }
                suspendFunctionClassTypeArguments = unboundParameterTypes + irFunction.descriptor.returnType!!
                superTypes += suspendFunctionClassDescriptor.defaultType.replace(suspendFunctionClassTypeArguments)

                functionClassDescriptor = kotlinPackageScope.getContributedClassifier(
                        Name.identifier("Function${numberOfParameters + 1}"), NoLookupLocation.FROM_BACKEND) as ClassDescriptor
                val continuationType = continuationClassDescriptor.defaultType.replace(listOf(irFunction.descriptor.returnType!!))
                functionClassTypeArguments = unboundParameterTypes + continuationType + context.builtIns.nullableAnyType
                superTypes += functionClassDescriptor.defaultType.replace(functionClassTypeArguments)

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
            coroutineClass = IrClassImpl(
                    startOffset = irFunction.startOffset,
                    endOffset   = irFunction.endOffset,
                    origin      = DECLARATION_ORIGIN_COROUTINE_IMPL,
                    descriptor  = coroutineClassDescriptor
            )

            coroutineClass.createParameterDeclarations()

            coroutineClassThis = coroutineClass.thisReceiver!!.symbol

            val overriddenMap = mutableMapOf<CallableMemberDescriptor, CallableMemberDescriptor>()
            val constructors = mutableSetOf<ClassConstructorDescriptor>()
            val coroutineConstructorBuilder = createConstructorBuilder()
            constructors.add(coroutineConstructorBuilder.symbol.descriptor)

            val doResumeFunctionDescriptor = coroutineImplClassDescriptor.unsubstitutedMemberScope
                    .getContributedFunctions(Name.identifier("doResume"), NoLookupLocation.FROM_BACKEND).single()
            val doResumeMethodBuilder = createDoResumeMethodBuilder(doResumeFunctionDescriptor)
            overriddenMap += doResumeFunctionDescriptor to doResumeMethodBuilder.symbol.descriptor

            var coroutineFactoryConstructorBuilder: SymbolWithIrBuilder<IrConstructorSymbol, IrConstructor>? = null
            var createMethodBuilder: SymbolWithIrBuilder<IrSimpleFunctionSymbol, IrSimpleFunction>? = null
            var invokeMethodBuilder: SymbolWithIrBuilder<IrSimpleFunctionSymbol, IrSimpleFunction>? = null
            if (functionReference != null) {
                // Suspend lambda - create factory methods.
                coroutineFactoryConstructorBuilder = createFactoryConstructorBuilder(boundFunctionParameters!!)
                constructors.add(coroutineFactoryConstructorBuilder.symbol.descriptor)

                val createFunctionDescriptor = coroutineImplClassDescriptor.unsubstitutedMemberScope
                        .getContributedFunctions(Name.identifier("create"), NoLookupLocation.FROM_BACKEND)
                        .atMostOne { it.valueParameters.size == unboundFunctionParameters!!.size + 1 }
                createMethodBuilder = createCreateMethodBuilder(
                        unboundArgs                    = unboundFunctionParameters!!,
                        superFunctionDescriptor        = createFunctionDescriptor,
                        coroutineConstructorSymbol     = coroutineConstructorBuilder.symbol)
                if (createFunctionDescriptor != null)
                    overriddenMap += createFunctionDescriptor to createMethodBuilder.symbol.descriptor

                val invokeFunctionDescriptor = functionClassDescriptor!!.getFunction("invoke", functionClassTypeArguments!!)
                val suspendInvokeFunctionDescriptor = suspendFunctionClassDescriptor!!.getFunction("invoke", suspendFunctionClassTypeArguments!!)
                invokeMethodBuilder = createInvokeMethodBuilder(
                        suspendFunctionInvokeFunctionDescriptor = suspendInvokeFunctionDescriptor,
                        functionInvokeFunctionDescriptor        = invokeFunctionDescriptor,
                        createFunctionSymbol                    = createMethodBuilder.symbol,
                        doResumeFunctionSymbol                  = doResumeMethodBuilder.symbol)
            }

            val inheritedFromCoroutineImpl = coroutineImplClassDescriptor.unsubstitutedMemberScope
                    .getContributedDescriptors()
                    .map { overriddenMap[it] ?: it.createFakeOverrideDescriptor(coroutineClassDescriptor) }
            val contributedDescriptors = (
                    inheritedFromCoroutineImpl + invokeMethodBuilder?.symbol?.descriptor
                    ).filterNotNull().toList()
            coroutineClassDescriptor.initialize(SimpleMemberScope(contributedDescriptors), constructors, null)

            coroutineConstructorBuilder.initialize()
            coroutineClass.declarations.add(coroutineConstructorBuilder.ir)

            coroutineFactoryConstructorBuilder?.let {
                it.initialize()
                coroutineClass.declarations.add(it.ir)
            }

            createMethodBuilder?.let {
                it.initialize()
                coroutineClass.declarations.add(it.ir)
            }

            invokeMethodBuilder?.let {
                it.initialize()
                coroutineClass.declarations.add(it.ir)
            }

            doResumeMethodBuilder.initialize()
            coroutineClass.declarations.add(doResumeMethodBuilder.ir)

            return BuiltCoroutine(
                    coroutineClass       = coroutineClass,
                    coroutineConstructor = coroutineFactoryConstructorBuilder?.ir
                            ?: coroutineConstructorBuilder.ir,
                    doResumeFunction     = doResumeMethodBuilder.ir)
        }

        private fun createConstructorBuilder()
                = object : SymbolWithIrBuilder<IrConstructorSymbol, IrConstructor>() {

            private val coroutineImplConstructorDescriptor = coroutineImplClassDescriptor.constructors.single()

            override fun buildSymbol() = IrConstructorSymbolImpl(
                    ClassConstructorDescriptorImpl.create(
                            /* containingDeclaration = */ coroutineClassDescriptor,
                            /* annotations           = */ Annotations.EMPTY,
                            /* isPrimary             = */ false,
                            /* source                = */ SourceElement.NO_SOURCE
                    )
            )

            override fun doInitialize() {
                val descriptor = symbol.descriptor as ClassConstructorDescriptorImpl
                val constructorParameters = (
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
                return IrConstructorImpl(
                        startOffset = startOffset,
                        endOffset   = endOffset,
                        origin      = DECLARATION_ORIGIN_COROUTINE_IMPL,
                        symbol      = symbol).apply {

                    createParameterDeclarations()

                    val irBuilder = context.createIrBuilder(symbol, startOffset, endOffset)
                    body = irBuilder.irBlockBody {
                        val completionParameter = valueParameters.last()
                        +IrDelegatingConstructorCallImpl(startOffset, endOffset, coroutineImplConstructorDescriptor).apply {
                            putValueArgument(0, irGet(completionParameter.symbol))
                        }
                        +IrInstanceInitializerCallImpl(startOffset, endOffset, coroutineClass.symbol)
                        functionParameters.forEachIndexed { index, parameter ->
                            +irSetField(irGet(coroutineClassThis), argumentToPropertiesMap[parameter]!!, irGet(valueParameters[index].symbol))
                        }
                    }
                }
            }
        }

        private fun createFactoryConstructorBuilder(boundParams: List<ParameterDescriptor>)
                = object : SymbolWithIrBuilder<IrConstructorSymbol, IrConstructor>() {

            private val coroutineImplConstructorDescriptor = coroutineImplClassDescriptor.constructors.single()

            override fun buildSymbol() = IrConstructorSymbolImpl(
                    ClassConstructorDescriptorImpl.create(
                            /* containingDeclaration = */ coroutineClassDescriptor,
                            /* annotations           = */ Annotations.EMPTY,
                            /* isPrimary             = */ false,
                            /* source                = */ SourceElement.NO_SOURCE
                    )
            )

            override fun doInitialize() {
                val descriptor = symbol.descriptor as ClassConstructorDescriptorImpl
                val constructorParameters = boundParams.mapIndexed { index, parameter ->
                    parameter.copyAsValueParameter(descriptor, index)
                }
                descriptor.initialize(constructorParameters, Visibilities.PUBLIC)
                descriptor.returnType = coroutineClassDescriptor.defaultType
            }

            override fun buildIr(): IrConstructor {
                val startOffset = irFunction.startOffset
                val endOffset = irFunction.endOffset
                return IrConstructorImpl(
                        startOffset = startOffset,
                        endOffset   = endOffset,
                        origin      = DECLARATION_ORIGIN_COROUTINE_IMPL,
                        symbol      = symbol).apply {

                    createParameterDeclarations()

                    val irBuilder = context.createIrBuilder(symbol, startOffset, endOffset)
                    body = irBuilder.irBlockBody {
                        +IrDelegatingConstructorCallImpl(startOffset, endOffset, coroutineImplConstructorDescriptor).apply {
                            putValueArgument(0, irNull()) // Completion.
                        }
                        +IrInstanceInitializerCallImpl(startOffset, endOffset, coroutineClass.symbol)
                        // Save all arguments to fields.
                        boundParams.forEachIndexed { index, parameter ->
                            +irSetField(irGet(coroutineClassThis), argumentToPropertiesMap[parameter]!!, irGet(valueParameters[index].symbol))
                        }
                    }
                }
            }
        }

        private fun createCreateMethodBuilder(unboundArgs: List<ParameterDescriptor>,
                                              superFunctionDescriptor: FunctionDescriptor?,
                                              coroutineConstructorSymbol: IrConstructorSymbol)
                = object: SymbolWithIrBuilder<IrSimpleFunctionSymbol, IrSimpleFunction>() {

            override fun buildSymbol() = IrSimpleFunctionSymbolImpl(
                    SimpleFunctionDescriptorImpl.create(
                            /* containingDeclaration = */ coroutineClassDescriptor,
                            /* annotations           = */ Annotations.EMPTY,
                            /* name                  = */ Name.identifier("create"),
                            /* kind                  = */ CallableMemberDescriptor.Kind.DECLARATION,
                            /* source                = */ SourceElement.NO_SOURCE
                    )
            )

            override fun doInitialize() {
                val descriptor = symbol.descriptor as SimpleFunctionDescriptorImpl
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

            override fun buildIr(): IrSimpleFunction {
                val startOffset = irFunction.startOffset
                val endOffset = irFunction.endOffset
                return IrFunctionImpl(
                        startOffset = startOffset,
                        endOffset   = endOffset,
                        origin      = DECLARATION_ORIGIN_COROUTINE_IMPL,
                        symbol      = symbol).apply {

                    createParameterDeclarations()

                    val irBuilder = context.createIrBuilder(symbol, startOffset, endOffset)
                    body = irBuilder.irBlockBody(startOffset, endOffset) {
                        +irReturn(
                                irCall(coroutineConstructorSymbol).apply {
                                    var unboundIndex = 0
                                    val unboundArgsSet = unboundArgs.toSet()
                                    functionParameters.map {
                                        if (unboundArgsSet.contains(it))
                                            irGet(valueParameters[unboundIndex++].symbol)
                                        else
                                            irGetField(irGet(coroutineClassThis), argumentToPropertiesMap[it]!!)
                                    }.forEachIndexed { index, argument ->
                                        putValueArgument(index, argument)
                                    }
                                    putValueArgument(functionParameters.size, irGet(valueParameters[unboundIndex].symbol))
                                    assert(unboundIndex == valueParameters.size - 1,
                                            { "Not all arguments of <create> are used" })
                                })
                    }
                }
            }
        }

        private fun createInvokeMethodBuilder(suspendFunctionInvokeFunctionDescriptor: FunctionDescriptor,
                                              functionInvokeFunctionDescriptor: FunctionDescriptor,
                                              createFunctionSymbol: IrFunctionSymbol,
                                              doResumeFunctionSymbol: IrFunctionSymbol)
                = object: SymbolWithIrBuilder<IrSimpleFunctionSymbol, IrSimpleFunction>() {

            override fun buildSymbol() = IrSimpleFunctionSymbolImpl(
                    SimpleFunctionDescriptorImpl.create(
                            /* containingDeclaration = */ coroutineClassDescriptor,
                            /* annotations           = */ Annotations.EMPTY,
                            /* name                  = */ Name.identifier("invoke"),
                            /* kind                  = */ CallableMemberDescriptor.Kind.DECLARATION,
                            /* source                = */ SourceElement.NO_SOURCE
                    )
            )

            override fun doInitialize() {
                val descriptor = symbol.descriptor as SimpleFunctionDescriptorImpl
                val valueParameters = createFunctionSymbol.descriptor.valueParameters
                        // Skip completion - invoke() already has it implicitly as a suspend function.
                        .take(createFunctionSymbol.descriptor.valueParameters.size - 1)
                        .map { it.copyAsValueParameter(descriptor, it.index) }

                descriptor.initialize(
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

            override fun buildIr(): IrSimpleFunction {
                val startOffset = irFunction.startOffset
                val endOffset = irFunction.endOffset
                return IrFunctionImpl(
                        startOffset = startOffset,
                        endOffset   = endOffset,
                        origin      = DECLARATION_ORIGIN_COROUTINE_IMPL,
                        symbol      = symbol).apply {

                    createParameterDeclarations()

                    val irBuilder = context.createIrBuilder(symbol, startOffset, endOffset)
                    body = irBuilder.irBlockBody(startOffset, endOffset) {
                        +irReturn(
                                irCall(doResumeFunctionSymbol).apply {
                                    dispatchReceiver = irCall(createFunctionSymbol).apply {
                                        dispatchReceiver = irGet(coroutineClassThis)
                                        valueParameters.forEachIndexed { index, parameter ->
                                            putValueArgument(index, irGet(parameter.symbol))
                                        }
                                        putValueArgument(valueParameters.size,
                                                irCall(getContinuationDescriptor.substitute(symbol.descriptor.returnType!!)))
                                    }
                                    putValueArgument(0, irUnit())       // value
                                    putValueArgument(1, irNull())       // exception
                                }
                        )
                    }
                }
            }
        }

        private fun buildPropertyWithBackingField(name: Name, type: KotlinType, isMutable: Boolean): IrFieldSymbol {
            val propertyBuilder = context.createPropertyWithBackingFieldBuilder(
                    startOffset = irFunction.startOffset,
                    endOffset   = irFunction.endOffset,
                    origin      = DECLARATION_ORIGIN_COROUTINE_IMPL,
                    owner       = coroutineClassDescriptor,
                    name        = name,
                    type        = type,
                    isMutable   = isMutable).apply {
                initialize()
            }

            coroutineClass.declarations.add(propertyBuilder.ir)
            return propertyBuilder.symbol
        }

        private fun createDoResumeMethodBuilder(doResumeFunctionDescriptor: FunctionDescriptor)
                = object: SymbolWithIrBuilder<IrSimpleFunctionSymbol, IrSimpleFunction>() {

            override fun buildSymbol() = IrSimpleFunctionSymbolImpl(
                    doResumeFunctionDescriptor.createOverriddenDescriptor(coroutineClassDescriptor)
            )

            override fun doInitialize() { }

            override fun buildIr(): IrSimpleFunction {
                val originalBody = irFunction.body!!
                val startOffset = irFunction.startOffset
                val endOffset = irFunction.endOffset
                val function = IrFunctionImpl(
                        startOffset = startOffset,
                        endOffset   = endOffset,
                        origin      = DECLARATION_ORIGIN_COROUTINE_IMPL,
                        symbol      = symbol).apply {

                    createParameterDeclarations()

                }

                dataArgument = function.valueParameters[0].symbol
                exceptionArgument = function.valueParameters[1].symbol
                suspendResult = IrVariableSymbolImpl(
                        IrTemporaryVariableDescriptorImpl(
                                containingDeclaration = irFunction.descriptor,
                                name                  = "suspendResult".synthesizedName,
                                outType               = context.builtIns.nullableAnyType,
                                isMutable             = true)
                )
                val label = coroutineClassDescriptor.unsubstitutedMemberScope
                        .getContributedVariables(Name.identifier("label"), NoLookupLocation.FROM_BACKEND).single()

                val irBuilder = context.createIrBuilder(function.symbol, startOffset, endOffset)
                function.body = irBuilder.irBlockBody(startOffset, endOffset) {

                    // Extract all suspend calls to temporaries in order to make correct jumps to them.
                    originalBody.transformChildrenVoid(ExpressionSlicer(label.type))

                    val liveLocals = computeLivenessAtSuspensionPoints(originalBody)

                    val immutableLiveLocals = liveLocals.values.flatten().filterNot { it.descriptor.isVar }.toSet()
                    val localsMap = immutableLiveLocals.associate {
                        // TODO: Remove .descriptor as soon as all symbols are bound.
                        it.descriptor to IrVariableSymbolImpl(
                                IrTemporaryVariableDescriptorImpl(
                                        containingDeclaration = irFunction.descriptor,
                                        name                  = it.descriptor.name,
                                        outType               = it.descriptor.type,
                                        isMutable             = true)
                        )
                    }

                    if (localsMap.isNotEmpty())
                        transformVariables(originalBody, localsMap)    // Make variables mutable in order to save/restore them.

                    val localToPropertyMap = mutableMapOf<IrVariableSymbol, IrFieldSymbol>()
                    // TODO: optimize by using the same property for different locals.
                    liveLocals.values.forEach { scope ->
                        scope.forEach {
                            localToPropertyMap.getOrPut(it) {
                                buildPropertyWithBackingField(it.descriptor.name, it.descriptor.type, true)
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
                            return irGetField(irGet(coroutineClassThis), capturedValue)
                        }

                        // Save/restore state at suspension points.
                        override fun visitExpression(expression: IrExpression): IrExpression {
                            expression.transformChildrenVoid(this)

                            val suspensionPoint = expression as? IrSuspensionPoint
                                    ?: return expression

                            suspensionPoint.transformChildrenVoid(object : IrElementTransformerVoid() {
                                override fun visitCall(expression: IrCall): IrExpression {
                                    expression.transformChildrenVoid(this)

                                    when (expression.symbol) {
                                        saveStateSymbol -> {
                                            val scope = liveLocals[suspensionPoint]!!
                                            return irBlock(expression) {
                                                scope.forEach {
                                                    +irSetField(irGet(coroutineClassThis), localToPropertyMap[it]!!, irGet(localsMap[it.descriptor] ?: it))
                                                }
                                                +irSet(irGet(coroutineClassThis), label, irGet(suspensionPoint.suspensionPointIdParameter.symbol))
                                            }
                                        }
                                        restoreStateSymbol -> {
                                            val scope = liveLocals[suspensionPoint]!!
                                            return irBlock(expression) {
                                                scope.forEach {
                                                    +irSetVar(localsMap[it.descriptor] ?: it, irGetField(irGet(coroutineClassThis), localToPropertyMap[it]!!))
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
                            suspensionPointId = irGet(irGet(coroutineClassThis), label),
                            result            = irBlock(startOffset, endOffset) {
                                +irThrowIfNotNull(exceptionArgument)    // Coroutine might start with an exception.
                                statements.forEach { +it }
                            })
                    if (irFunction.descriptor.returnType!!.isUnit())
                        +irReturn(irUnit())                             // Insert explicit return for Unit functions.
                }
                return function
            }
        }

        private fun transformVariables(element: IrElement, variablesMap: Map<VariableDescriptor, IrVariableSymbol>) {
            element.transformChildrenVoid(object: IrElementTransformerVoid() {

                override fun visitGetValue(expression: IrGetValue): IrExpression {
                    expression.transformChildrenVoid(this)

                    val newVariable = variablesMap[expression.symbol.descriptor]
                            ?: return expression

                    return IrGetValueImpl(
                            startOffset = expression.startOffset,
                            endOffset   = expression.endOffset,
                            symbol      = newVariable,
                            origin      = expression.origin)
                }

                override fun visitSetVariable(expression: IrSetVariable): IrExpression {
                    expression.transformChildrenVoid(this)

                    val newVariable = variablesMap[expression.symbol.descriptor]
                            ?: return expression

                    return IrSetVariableImpl(
                            startOffset = expression.startOffset,
                            endOffset   = expression.endOffset,
                            symbol      = newVariable,
                            value       = expression.value,
                            origin      = expression.origin)
                }

                override fun visitVariable(declaration: IrVariable): IrStatement {
                    declaration.transformChildrenVoid(this)

                    val newVariable = variablesMap[declaration.symbol.descriptor]
                            ?: return declaration

                    return IrVariableImpl(
                            startOffset = declaration.startOffset,
                            endOffset   = declaration.endOffset,
                            origin      = declaration.origin,
                            symbol      = newVariable).apply {
                        initializer = declaration.initializer
                    }
                }
            })
        }

        private fun computeLivenessAtSuspensionPoints(body: IrBody): Map<IrSuspensionPoint, List<IrVariableSymbol>> {
            // TODO: data flow analysis.
            // Just save all visible for now.
            val result = mutableMapOf<IrSuspensionPoint, List<IrVariableSymbol>>()
            body.acceptChildrenVoid(object: VariablesScopeTracker() {

                override fun visitExpression(expression: IrExpression) {
                    val suspensionPoint = expression as? IrSuspensionPoint
                    if (suspensionPoint == null) {
                        super.visitExpression(expression)
                        return
                    }

                    suspensionPoint.result.acceptChildrenVoid(this)
                    suspensionPoint.resumeResult.acceptChildrenVoid(this)

                    val visibleVariables = mutableListOf<IrVariableSymbol>()
                    scopeStack.forEach { visibleVariables += it }
                    result.put(suspensionPoint, visibleVariables)
                }
            })

            return result
        }

        // These are marker descriptors to split up the lowering on two parts.
        private val saveStateSymbol = IrSimpleFunctionSymbolImpl(
                SimpleFunctionDescriptorImpl.create(
                        irFunction.descriptor,
                        Annotations.EMPTY,
                        "saveState".synthesizedName,
                        CallableMemberDescriptor.Kind.SYNTHESIZED,
                        SourceElement.NO_SOURCE).apply {
                    initialize(null, null, emptyList(), emptyList(), context.builtIns.unitType, Modality.ABSTRACT, Visibilities.PRIVATE)
                }
        )

        private val restoreStateSymbol = IrSimpleFunctionSymbolImpl(
                SimpleFunctionDescriptorImpl.create(
                        irFunction.descriptor,
                        Annotations.EMPTY,
                        "restoreState".synthesizedName,
                        CallableMemberDescriptor.Kind.SYNTHESIZED,
                        SourceElement.NO_SOURCE).apply {
                    initialize(null, null, emptyList(), emptyList(), context.builtIns.unitType, Modality.ABSTRACT, Visibilities.PRIVATE)
                }
        )

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
                val irBuilder = context.createIrBuilder(irFunction.symbol, expression.startOffset, expression.endOffset)
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
                            val tmp = IrVariableSymbolImpl(
                                    IrTemporaryVariableDescriptorImpl(
                                            containingDeclaration = irFunction.descriptor,
                                            name                  = "tmp${tempIndex++}".synthesizedName,
                                            outType               = transformedChild.type)
                            )
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
                                            +irCall(saveStateSymbol)
                                            +lastChild
                                        } else {
                                            val tmp = IrVariableSymbolImpl(
                                                    IrTemporaryVariableDescriptorImpl(
                                                            containingDeclaration = irFunction.descriptor,
                                                            name                  = "tmp${tempIndex++}".synthesizedName,
                                                            outType               = lastChild.type)
                                            )
                                            +irVar(tmp, lastChild)
                                            +irCall(saveStateSymbol)
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
                                    +irCall(saveStateSymbol)
                                +irSetVar(suspendResult, suspendCall)
                                +irReturnIfSuspended(suspendResult)
                                +irGet(suspendResult)
                            },
                            resumeResult               = irBlock(startOffset, endOffset) {
                                +irCall(restoreStateSymbol)
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

        private fun IrBuilderWithScope.irVar(symbol: IrVariableSymbol, initializer: IrExpression?) =
                IrVariableImpl(startOffset, endOffset, DECLARATION_ORIGIN_COROUTINE_IMPL, symbol).apply {
                    this.initializer = initializer
                }

        private fun IrBuilderWithScope.irReturnIfSuspended(value: IrValueSymbol) =
                irIfThen(irEqeqeq(irGet(value), irGet(COROUTINE_SUSPENDED)),
                        irReturn(irGet(value)))

        private fun IrBuilderWithScope.irThrowIfNotNull(exception: IrValueSymbol) =
                irIfThen(irNot(irEqeqeq(irGet(exception), irNull())),
                        irThrow(irImplicitCast(irGet(exception), exception.descriptor.type.makeNotNullable())))
    }

    private open class VariablesScopeTracker: IrElementVisitorVoid {

        protected val scopeStack = mutableListOf<MutableSet<IrVariableSymbol>>(mutableSetOf())

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
            scopeStack.push(mutableSetOf())
            super.visitCatch(aCatch)
            scopeStack.pop()
        }

        override fun visitVariable(declaration: IrVariable) {
            super.visitVariable(declaration)
            scopeStack.peek()!!.add(declaration.symbol)
        }
    }
}