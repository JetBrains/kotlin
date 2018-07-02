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

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.descriptors.getFunction
import org.jetbrains.kotlin.backend.common.ir.createOverriddenDescriptor
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.konan.irasdescriptors.typeWith
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ClassConstructorDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
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
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.storage.LockBasedStorageManager

internal class SuspendFunctionsLowering(val context: Context): FileLoweringPass {

    private object STATEMENT_ORIGIN_COROUTINE_IMPL : IrStatementOriginImpl("COROUTINE_IMPL")
    private object DECLARATION_ORIGIN_COROUTINE_IMPL : IrDeclarationOriginImpl("COROUTINE_IMPL")

    private val builtCoroutines = mutableMapOf<FunctionDescriptor, BuiltCoroutine>()
    private val suspendLambdas = mutableMapOf<FunctionDescriptor, IrFunctionReference>()

    override fun lower(irFile: IrFile) {
        markSuspendLambdas(irFile)
        buildCoroutines(irFile)
        transformCallableReferencesToSuspendLambdas(irFile)
    }

    private fun buildCoroutines(irFile: IrFile) {
        irFile.declarations.transformFlat(::tryTransformSuspendFunction)
        irFile.acceptVoid(object: IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitClass(declaration: IrClass) {
                declaration.acceptChildrenVoid(this)
                declaration.declarations.transformFlat(::tryTransformSuspendFunction)
            }
        })
    }

    private fun tryTransformSuspendFunction(element: IrElement) =
            if (element is IrFunction && element.descriptor.isSuspend && element.descriptor.modality != Modality.ABSTRACT)
                transformSuspendFunction(element, suspendLambdas[element.descriptor])
            else null

    private fun markSuspendLambdas(irElement: IrElement) {
        irElement.acceptChildrenVoid(object : IrElementVisitorVoid {
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

    private fun transformCallableReferencesToSuspendLambdas(irElement: IrElement) {
        irElement.transformChildrenVoid(object : IrElementTransformerVoid() {

            override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
                expression.transformChildrenVoid(this)

                val descriptor = expression.descriptor
                if (!descriptor.isSuspend)
                    return expression
                val coroutine = builtCoroutines[descriptor]
                        ?: throw Error("The coroutine for $descriptor has not been built")
                val constructorParameters = coroutine.coroutineConstructor.valueParameters
                val expressionArguments = expression.getArguments().map { it.second }
                assert(constructorParameters.size == expressionArguments.size,
                        { "Inconsistency between callable reference to suspend lambda and the corresponding coroutine" })
                val irBuilder = context.createIrBuilder(expression.symbol, expression.startOffset, expression.endOffset)
                irBuilder.run {
                    return irCall(coroutine.coroutineConstructor.symbol).apply {
                        expressionArguments.forEachIndexed { index, argument ->
                            putValueArgument(index, argument)
                        }
                    }
                }
            }
        })
    }

    private sealed class SuspendFunctionKind {
        object NO_SUSPEND_CALLS : SuspendFunctionKind()
        class DELEGATING(val delegatingCall: IrCall) : SuspendFunctionKind()
        object NEEDS_STATE_MACHINE : SuspendFunctionKind()
    }

    private fun transformSuspendFunction(irFunction: IrFunction, functionReference: IrFunctionReference?): List<IrDeclaration>? {
        val suspendFunctionKind = getSuspendFunctionKind(irFunction)
        return when (suspendFunctionKind) {
            is SuspendFunctionKind.NO_SUSPEND_CALLS -> {
                null                                                            // No suspend function calls - just an ordinary function.
            }

            is SuspendFunctionKind.DELEGATING -> {                              // Calls another suspend function at the end.
                removeReturnIfSuspendedCallAndSimplifyDelegatingCall(
                        irFunction, suspendFunctionKind.delegatingCall)
                null                                                            // No need in state machine.
            }

            is SuspendFunctionKind.NEEDS_STATE_MACHINE -> {
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
                var value: IrElement = lastStatement
                /*
                 * Check if matches this pattern:
                 * block/return {
                 *     block/return {
                 *         .. suspendCall()
                 *     }
                 * }
                 */
                loop@while (true) {
                    when {
                        value is IrBlock && value.statements.size == 1 -> value = value.statements.first()
                        value is IrReturn -> value = value.value
                        else -> break@loop
                    }
                }
                value as? IrCall
            }
            else -> null
        }
        val suspendCallAtEnd = lastCall != null && lastCall.descriptor.isSuspend     // Suspend call.
        return when {
            numberOfSuspendCalls == 0   -> SuspendFunctionKind.NO_SUSPEND_CALLS
            numberOfSuspendCalls == 1
                    && suspendCallAtEnd -> SuspendFunctionKind.DELEGATING(lastCall!!)
            else                        -> SuspendFunctionKind.NEEDS_STATE_MACHINE
        }
    }

    private val symbols = context.ir.symbols
    private val getContinuation = symbols.getContinuation.owner
    private val continuationClassSymbol = getContinuation.returnType.classifierOrFail as IrClassSymbol
    private val returnIfSuspendedDescriptor = context.getInternalFunctions("returnIfSuspended").single()

    private fun removeReturnIfSuspendedCallAndSimplifyDelegatingCall(irFunction: IrFunction, delegatingCall: IrCall) {
        val returnValue =
                if (delegatingCall.descriptor.original == returnIfSuspendedDescriptor)
                    delegatingCall.getValueArgument(0)!!
                else delegatingCall
        context.createIrBuilder(irFunction.symbol).run {
            val statements = (irFunction.body as IrBlockBody).statements
            val lastStatement = statements.last()
            assert (lastStatement == delegatingCall || lastStatement is IrReturn) { "Unexpected statement $lastStatement" }
            statements[statements.size - 1] = irReturn(returnValue)
        }
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
                                val functionParameters = irFunction.explicitParameters
                                functionParameters.forEachIndexed { index, argument ->
                                    putValueArgument(index, irGet(argument))
                                }
                                putValueArgument(functionParameters.size,
                                        irCall(getContinuation, listOf(irFunction.returnType)))
                            }
                            putValueArgument(0, irGetObject(symbols.unit)) // value
                            putValueArgument(1, irNull()) // exception
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
    private val KOTLIN_FQ_NAME                = FqName("kotlin")

    private val coroutinesScope           = context.irModule!!.descriptor.getPackage(COROUTINES_FQ_NAME).memberScope
    private val kotlinPackageScope        = context.irModule!!.descriptor.getPackage(KOTLIN_FQ_NAME).memberScope

    private inner class CoroutineBuilder(val irFunction: IrFunction, val functionReference: IrFunctionReference?) {

        private val functionParameters = irFunction.explicitParameters
        private val boundFunctionParameters = functionReference?.getArgumentsWithIr()?.map { it.first }
        private val unboundFunctionParameters = boundFunctionParameters?.let { functionParameters - it }

        private var tempIndex = 0
        private var suspensionPointIdIndex = 0
        private lateinit var suspendResult: IrVariable
        private lateinit var dataArgument: IrValueParameter
        private lateinit var exceptionArgument: IrValueParameter
        private lateinit var coroutineClassDescriptor: ClassDescriptorImpl
        private lateinit var coroutineClass: IrClassImpl
        private lateinit var coroutineClassThis: IrValueParameter
        private lateinit var argumentToPropertiesMap: Map<ParameterDescriptor, IrField>

        private val coroutineImplSymbol = symbols.coroutineImpl
        private val coroutineImplConstructorSymbol = coroutineImplSymbol.constructors.single()
        private val coroutineImplClassDescriptor = coroutineImplSymbol.descriptor
        private val create1Function = coroutineImplSymbol.owner.simpleFunctions()
                .single { it.name.asString() == "create" && it.valueParameters.size == 1 }
        private val create1CompletionParameter = create1Function.valueParameters[0]

        private val coroutineImplLabelGetterSymbol = coroutineImplSymbol.getPropertyGetter("label")!!
        private val coroutineImplLabelSetterSymbol = coroutineImplSymbol.getPropertySetter("label")!!

        fun build(): BuiltCoroutine {
            val superTypes = mutableListOf<IrType>(coroutineImplSymbol.owner.defaultType)
            var suspendFunctionClass: IrClass? = null
            var functionClass: IrClass? = null
            var suspendFunctionClassTypeArguments: List<IrType>? = null
            var functionClassTypeArguments: List<IrType>? = null
            if (unboundFunctionParameters != null) {
                // Suspend lambda inherits SuspendFunction.
                val numberOfParameters = unboundFunctionParameters.size
                suspendFunctionClass = context.ir.symbols.suspendFunctions[numberOfParameters].owner
                val unboundParameterTypes = unboundFunctionParameters.map { it.type }
                suspendFunctionClassTypeArguments = unboundParameterTypes + irFunction.returnType
                superTypes += suspendFunctionClass.typeWith(suspendFunctionClassTypeArguments)

                functionClass = context.ir.symbols.functions[numberOfParameters + 1].owner
                val continuationType = continuationClassSymbol.typeWith(irFunction.returnType)
                functionClassTypeArguments = unboundParameterTypes + continuationType + context.irBuiltIns.anyNType
                superTypes += functionClass.typeWith(functionClassTypeArguments)

            }
            coroutineClassDescriptor = ClassDescriptorImpl(
                    /* containingDeclaration = */ irFunction.descriptor.containingDeclaration,
                    /* name                  = */ "${irFunction.descriptor.name}\$${coroutineId++}".synthesizedName,
                    /* modality              = */ Modality.FINAL,
                    /* kind                  = */ ClassKind.CLASS,
                    /* superTypes            = */ superTypes.map { it.toKotlinType() },
                    /* source                = */ SourceElement.NO_SOURCE,
                    /* isExternal            = */ false,
                    /* storageManager        = */ LockBasedStorageManager.NO_LOCKS
            ).also {
                it.initialize(stub("coroutine class"), stub("coroutine class constructors"), null)
            }
            coroutineClass = IrClassImpl(
                    startOffset = irFunction.startOffset,
                    endOffset   = irFunction.endOffset,
                    origin      = DECLARATION_ORIGIN_COROUTINE_IMPL,
                    descriptor  = coroutineClassDescriptor
            )
            coroutineClass.parent = irFunction.parent
            coroutineClass.createParameterDeclarations()
            coroutineClassThis = coroutineClass.thisReceiver!!


            val overriddenMap = mutableMapOf<CallableMemberDescriptor, CallableMemberDescriptor>()
            val coroutineConstructorBuilder = createConstructorBuilder()
            coroutineConstructorBuilder.initialize()

            val doResumeFunction = coroutineImplSymbol.owner.simpleFunctions()
                    .single { it.name.asString() == "doResume" }
            val doResumeMethodBuilder = createDoResumeMethodBuilder(doResumeFunction, coroutineClass)
            doResumeMethodBuilder.initialize()
            overriddenMap += doResumeFunction.descriptor to doResumeMethodBuilder.symbol.descriptor

            var coroutineFactoryConstructorBuilder: SymbolWithIrBuilder<IrConstructorSymbol, IrConstructor>? = null
            var createMethodBuilder: SymbolWithIrBuilder<IrSimpleFunctionSymbol, IrSimpleFunction>? = null
            var invokeMethodBuilder: SymbolWithIrBuilder<IrSimpleFunctionSymbol, IrSimpleFunction>? = null
            if (functionReference != null) {
                // Suspend lambda - create factory methods.
                coroutineFactoryConstructorBuilder = createFactoryConstructorBuilder(boundFunctionParameters!!)

                val createFunctionDescriptor = coroutineImplClassDescriptor.unsubstitutedMemberScope
                        .getContributedFunctions(Name.identifier("create"), NoLookupLocation.FROM_BACKEND)
                        .atMostOne { it.valueParameters.size == unboundFunctionParameters!!.size + 1 }
                createMethodBuilder = createCreateMethodBuilder(
                        unboundArgs                    = unboundFunctionParameters!!,
                        superFunctionDescriptor        = createFunctionDescriptor,
                        coroutineConstructor           = coroutineConstructorBuilder.ir,
                        coroutineClass                 = coroutineClass)
                createMethodBuilder.initialize()
                if (createFunctionDescriptor != null)
                    overriddenMap += createFunctionDescriptor to createMethodBuilder.symbol.descriptor

                val invokeFunctionDescriptor = functionClass!!.descriptor
                        .getFunction("invoke", functionClassTypeArguments!!.map { it.toKotlinType() })
                val suspendInvokeFunctionDescriptor = suspendFunctionClass!!.descriptor
                        .getFunction("invoke", suspendFunctionClassTypeArguments!!.map { it.toKotlinType() })
                invokeMethodBuilder = createInvokeMethodBuilder(
                        suspendFunctionInvokeFunctionDescriptor = suspendInvokeFunctionDescriptor,
                        functionInvokeFunctionDescriptor        = invokeFunctionDescriptor,
                        createFunction                          = createMethodBuilder.ir,
                        doResumeFunction                        = doResumeMethodBuilder.ir,
                        coroutineClass                          = coroutineClass)
            }

            coroutineClass.addChild(coroutineConstructorBuilder.ir)

            coroutineFactoryConstructorBuilder?.let {
                it.initialize()
                coroutineClass.addChild(it.ir)
            }

            createMethodBuilder?.let {
                coroutineClass.addChild(it.ir)
            }

            invokeMethodBuilder?.let {
                it.initialize()
                coroutineClass.addChild(it.ir)
            }

            coroutineClass.addChild(doResumeMethodBuilder.ir)

            coroutineClass.setSuperSymbolsAndAddFakeOverrides(superTypes)

            return BuiltCoroutine(
                    coroutineClass       = coroutineClass,
                    coroutineConstructor = coroutineFactoryConstructorBuilder?.ir
                            ?: coroutineConstructorBuilder.ir,
                    doResumeFunction     = doResumeMethodBuilder.ir)
        }

        private fun createConstructorBuilder()
                = object : SymbolWithIrBuilder<IrConstructorSymbol, IrConstructor>() {

            override fun buildSymbol() = IrConstructorSymbolImpl(
                    ClassConstructorDescriptorImpl.create(
                            /* containingDeclaration = */ coroutineClassDescriptor,
                            /* annotations           = */ Annotations.EMPTY,
                            /* isPrimary             = */ false,
                            /* source                = */ SourceElement.NO_SOURCE
                    )
            )

            private lateinit var constructorParameters: List<IrValueParameter>

            override fun doInitialize() {
                val descriptor = symbol.descriptor as ClassConstructorDescriptorImpl
                constructorParameters = (
                        functionParameters
                        + coroutineImplConstructorSymbol.owner.valueParameters[0] // completion.
                        ).mapIndexed { index, parameter ->

                    val parameterDescriptor = parameter.descriptor.copyAsValueParameter(descriptor, index)
                    parameter.copy(parameterDescriptor)
                }

                descriptor.initialize(
                        constructorParameters.map { it.descriptor as ValueParameterDescriptor },
                        Visibilities.PUBLIC
                )
                descriptor.returnType = coroutineClassDescriptor.defaultType
            }

            override fun buildIr(): IrConstructor {
                // Save all arguments to fields.
                argumentToPropertiesMap = functionParameters.associate {
                    it.descriptor to addField(it.name, it.type, false)
                }

                val startOffset = irFunction.startOffset
                val endOffset = irFunction.endOffset
                return IrConstructorImpl(
                        startOffset = startOffset,
                        endOffset   = endOffset,
                        origin      = DECLARATION_ORIGIN_COROUTINE_IMPL,
                        symbol      = symbol).apply {

                    returnType  = coroutineClass.defaultType

                    this.valueParameters += constructorParameters

                    val irBuilder = context.createIrBuilder(symbol, startOffset, endOffset)
                    body = irBuilder.irBlockBody {
                        val completionParameter = valueParameters.last()
                        +IrDelegatingConstructorCallImpl(startOffset, endOffset,
                                context.irBuiltIns.unitType,
                                coroutineImplConstructorSymbol, coroutineImplConstructorSymbol.descriptor).apply {
                            putValueArgument(0, irGet(completionParameter))
                        }
                        +IrInstanceInitializerCallImpl(startOffset, endOffset, coroutineClass.symbol, context.irBuiltIns.unitType)
                        functionParameters.forEachIndexed { index, parameter ->
                            +irSetField(
                                    irGet(coroutineClassThis),
                                    argumentToPropertiesMap[parameter.descriptor]!!,
                                    irGet(valueParameters[index])
                            )
                        }
                    }
                }
            }
        }

        private fun createFactoryConstructorBuilder(boundParams: List<IrValueParameter>)
                = object : SymbolWithIrBuilder<IrConstructorSymbol, IrConstructor>() {

            override fun buildSymbol() = IrConstructorSymbolImpl(
                    ClassConstructorDescriptorImpl.create(
                            /* containingDeclaration = */ coroutineClassDescriptor,
                            /* annotations           = */ Annotations.EMPTY,
                            /* isPrimary             = */ false,
                            /* source                = */ SourceElement.NO_SOURCE
                    )
            )

            lateinit var constructorParameters: List<IrValueParameter>

            override fun doInitialize() {
                val descriptor = symbol.descriptor as ClassConstructorDescriptorImpl
                constructorParameters = boundParams.mapIndexed { index, parameter ->
                    val parameterDescriptor = parameter.descriptor.copyAsValueParameter(descriptor, index)
                    parameter.copy(parameterDescriptor)
                }
                descriptor.initialize(
                        constructorParameters.map { it.descriptor as ValueParameterDescriptor },
                        Visibilities.PUBLIC
                )
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

                    returnType = coroutineClass.defaultType

                    this.valueParameters += constructorParameters

                    val irBuilder = context.createIrBuilder(symbol, startOffset, endOffset)
                    body = irBuilder.irBlockBody {
                        +IrDelegatingConstructorCallImpl(startOffset, endOffset, context.irBuiltIns.unitType,
                                coroutineImplConstructorSymbol, coroutineImplConstructorSymbol.descriptor).apply {
                            putValueArgument(0, irNull()) // Completion.
                        }
                        +IrInstanceInitializerCallImpl(startOffset, endOffset, coroutineClass.symbol,
                                context.irBuiltIns.unitType)
                        // Save all arguments to fields.
                        boundParams.forEachIndexed { index, parameter ->
                            +irSetField(irGet(coroutineClassThis), argumentToPropertiesMap[parameter.descriptor]!!,
                                    irGet(valueParameters[index]))
                        }
                    }
                }
            }
        }

        private fun createCreateMethodBuilder(unboundArgs: List<IrValueParameter>,
                                              superFunctionDescriptor: FunctionDescriptor?,
                                              coroutineConstructor: IrConstructor,
                                              coroutineClass: IrClass)
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

            lateinit var parameters: List<IrValueParameter>

            override fun doInitialize() {
                val descriptor = symbol.descriptor as SimpleFunctionDescriptorImpl
                parameters = (
                        unboundArgs + create1CompletionParameter
                        ).mapIndexed { index, parameter ->
                    parameter.copy(parameter.descriptor.copyAsValueParameter(descriptor, index))
                }

                descriptor.initialize(
                        /* receiverParameterType        = */ null,
                        /* dispatchReceiverParameter    = */ coroutineClassDescriptor.thisAsReceiverParameter,
                        /* typeParameters               = */ emptyList(),
                        /* unsubstitutedValueParameters = */ parameters.map { it.descriptor as ValueParameterDescriptor },
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

                    returnType  = coroutineClass.defaultType
                    parent = coroutineClass

                    this.valueParameters += parameters
                    this.createDispatchReceiverParameter()

                    val thisReceiver = this.dispatchReceiverParameter!!

                    val irBuilder = context.createIrBuilder(symbol, startOffset, endOffset)
                    body = irBuilder.irBlockBody(startOffset, endOffset) {
                        +irReturn(
                                irCall(coroutineConstructor).apply {
                                    var unboundIndex = 0
                                    val unboundArgsSet = unboundArgs.toSet()
                                    functionParameters.map {
                                        if (unboundArgsSet.contains(it))
                                            irGet(valueParameters[unboundIndex++])
                                        else
                                            irGetField(irGet(thisReceiver), argumentToPropertiesMap[it.descriptor]!!)
                                    }.forEachIndexed { index, argument ->
                                        putValueArgument(index, argument)
                                    }
                                    putValueArgument(functionParameters.size, irGet(valueParameters[unboundIndex]))
                                    assert(unboundIndex == valueParameters.size - 1,
                                            { "Not all arguments of <create> are used" })
                                })
                    }
                }
            }
        }

        private fun createInvokeMethodBuilder(suspendFunctionInvokeFunctionDescriptor: FunctionDescriptor,
                                              functionInvokeFunctionDescriptor: FunctionDescriptor,
                                              createFunction: IrFunction,
                                              doResumeFunction: IrFunction,
                                              coroutineClass: IrClass)
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

            lateinit var parameters: List<IrValueParameter>

            override fun doInitialize() {
                val descriptor = symbol.descriptor as SimpleFunctionDescriptorImpl
                parameters = createFunction.valueParameters
                        // Skip completion - invoke() already has it implicitly as a suspend function.
                        .take(createFunction.valueParameters.size - 1)
                        .map { it.copy(it.descriptor.copyAsValueParameter(descriptor, it.index)) }

                descriptor.initialize(
                        /* receiverParameterType        = */ null,
                        /* dispatchReceiverParameter    = */ coroutineClassDescriptor.thisAsReceiverParameter,
                        /* typeParameters               = */ emptyList(),
                        /* unsubstitutedValueParameters = */ parameters.map { it.descriptor as ValueParameterDescriptor },
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

                    returnType  = irFunction.returnType
                    parent = coroutineClass

                    valueParameters += parameters
                    this.createDispatchReceiverParameter()

                    val thisReceiver = this.dispatchReceiverParameter!!

                    val irBuilder = context.createIrBuilder(symbol, startOffset, endOffset)
                    body = irBuilder.irBlockBody(startOffset, endOffset) {
                        +irReturn(
                                irCall(doResumeFunction).apply {
                                    dispatchReceiver = irCall(createFunction).apply {
                                        dispatchReceiver = irGet(thisReceiver)
                                        valueParameters.forEachIndexed { index, parameter ->
                                            putValueArgument(index, irGet(parameter))
                                        }
                                        putValueArgument(valueParameters.size,
                                                irCall(getContinuation, listOf(returnType)))
                                    }
                                    putValueArgument(0, irGetObject(symbols.unit))       // value
                                    putValueArgument(1, irNull())       // exception
                                }
                        )
                    }
                }
            }
        }

        private fun addField(name: Name, type: IrType, isMutable: Boolean): IrField = createField(
                irFunction.startOffset,
                irFunction.endOffset,
                type,
                name,
                isMutable,
                DECLARATION_ORIGIN_COROUTINE_IMPL,
                coroutineClassDescriptor
        ).also {
            coroutineClass.addChild(it)
        }

        private fun createDoResumeMethodBuilder(doResumeFunction: IrFunction, coroutineClass: IrClass)
                = object: SymbolWithIrBuilder<IrSimpleFunctionSymbol, IrSimpleFunction>() {

            override fun buildSymbol() = IrSimpleFunctionSymbolImpl(
                    doResumeFunction.descriptor.createOverriddenDescriptor(coroutineClassDescriptor)
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

                    returnType  = context.irBuiltIns.anyNType
                    parent = coroutineClass

                    this.createDispatchReceiverParameter()

                    doResumeFunction.valueParameters.mapIndexedTo(this.valueParameters) { index, it ->
                        it.copy(descriptor.valueParameters[index])
                    }

                }

                dataArgument = function.valueParameters[0]
                exceptionArgument = function.valueParameters[1]

                val label = coroutineImplSymbol.owner.declarations.filterIsInstance<IrProperty>()
                        .single { it.name.asString() == "label" }

                val irBuilder = context.createIrBuilder(function.symbol, startOffset, endOffset)
                function.body = irBuilder.irBlockBody(startOffset, endOffset) {

                    suspendResult = irVar(IrTemporaryVariableDescriptorImpl(
                                    containingDeclaration = irFunction.descriptor,
                                    name                  = "suspendResult".synthesizedName,
                                    outType               = context.builtIns.nullableAnyType,
                                    isMutable             = true)
                    , context.irBuiltIns.anyNType)

                    // Extract all suspend calls to temporaries in order to make correct jumps to them.
                    originalBody.transformChildrenVoid(ExpressionSlicer(label.getter!!.returnType))

                    val liveLocals = computeLivenessAtSuspensionPoints(originalBody)

                    val immutableLiveLocals = liveLocals.values.flatten().filterNot { it.descriptor.isVar }.toSet()
                    val localsMap = immutableLiveLocals.associate {
                        // TODO: Remove .descriptor as soon as all symbols are bound.
                        val symbol = IrVariableSymbolImpl(
                                IrTemporaryVariableDescriptorImpl(
                                        containingDeclaration = irFunction.descriptor,
                                        name = it.descriptor.name,
                                        outType = it.descriptor.type,
                                        isMutable = true)
                        )

                        val variable = IrVariableImpl(
                                startOffset = it.startOffset,
                                endOffset   = it.endOffset,
                                origin      = it.origin,
                                symbol      = symbol,
                                type        = it.type
                        )

                        it.descriptor to variable
                    }

                    if (localsMap.isNotEmpty())
                        transformVariables(originalBody, localsMap)    // Make variables mutable in order to save/restore them.

                    val localToPropertyMap = mutableMapOf<IrVariableSymbol, IrField>()
                    // TODO: optimize by using the same property for different locals.
                    liveLocals.values.forEach { scope ->
                        scope.forEach {
                            localToPropertyMap.getOrPut(it.symbol) {
                                addField(it.descriptor.name, it.type, true)
                            }
                        }
                    }

                    originalBody.transformChildrenVoid(object : IrElementTransformerVoid() {

                        private val thisReceiver = function.dispatchReceiverParameter!!

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
                            return irGetField(irGet(thisReceiver), capturedValue)
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
                                        saveState.symbol -> {
                                            val scope = liveLocals[suspensionPoint]!!
                                            return irBlock(expression) {
                                                scope.forEach {
                                                    val variable = localsMap[it.descriptor] ?: it
                                                    +irSetField(irGet(thisReceiver), localToPropertyMap[it.symbol]!!, irGet(variable))
                                                }
                                                +irCall(coroutineImplLabelSetterSymbol).apply {
                                                    dispatchReceiver = irGet(thisReceiver)
                                                    putValueArgument(0, irGet(suspensionPoint.suspensionPointIdParameter))
                                                }
                                            }
                                        }
                                        restoreState.symbol -> {
                                            val scope = liveLocals[suspensionPoint]!!
                                            return irBlock(expression) {
                                                scope.forEach {
                                                    +irSetVar(localsMap[it.descriptor] ?: it, irGetField(irGet(thisReceiver), localToPropertyMap[it.symbol]!!))
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
                    +suspendResult
                    +IrSuspendableExpressionImpl(
                            startOffset       = startOffset,
                            endOffset         = endOffset,
                            type              = context.irBuiltIns.unitType,
                            suspensionPointId = irCall(coroutineImplLabelGetterSymbol).apply {
                                dispatchReceiver = irGet(function.dispatchReceiverParameter!!)
                            },
                            result            = irBlock(startOffset, endOffset) {
                                +irThrowIfNotNull(exceptionArgument)    // Coroutine might start with an exception.
                                statements.forEach { +it }
                            })
                    if (irFunction.returnType.isUnit())
                        +irReturn(irGetObject(symbols.unit))                             // Insert explicit return for Unit functions.
                }
                return function
            }
        }

        private fun transformVariables(element: IrElement, variablesMap: Map<VariableDescriptor, IrVariable>) {
            element.transformChildrenVoid(object: IrElementTransformerVoid() {

                override fun visitGetValue(expression: IrGetValue): IrExpression {
                    expression.transformChildrenVoid(this)

                    val newVariable = variablesMap[expression.symbol.descriptor]
                            ?: return expression

                    return IrGetValueImpl(
                            startOffset = expression.startOffset,
                            endOffset   = expression.endOffset,
                            type        = newVariable.type,
                            symbol      = newVariable.symbol,
                            origin      = expression.origin)
                }

                override fun visitSetVariable(expression: IrSetVariable): IrExpression {
                    expression.transformChildrenVoid(this)

                    val newVariable = variablesMap[expression.symbol.descriptor]
                            ?: return expression

                    return IrSetVariableImpl(
                            startOffset = expression.startOffset,
                            endOffset   = expression.endOffset,
                            type        = context.irBuiltIns.unitType,
                            symbol      = newVariable.symbol,
                            value       = expression.value,
                            origin      = expression.origin)
                }

                override fun visitVariable(declaration: IrVariable): IrStatement {
                    declaration.transformChildrenVoid(this)

                    val newVariable = variablesMap[declaration.symbol.descriptor]
                            ?: return declaration

                    newVariable.initializer = declaration.initializer

                    return newVariable
                }
            })
        }

        private fun computeLivenessAtSuspensionPoints(body: IrBody): Map<IrSuspensionPoint, List<IrVariable>> {
            // TODO: data flow analysis.
            // Just save all visible for now.
            val result = mutableMapOf<IrSuspensionPoint, List<IrVariable>>()
            body.acceptChildrenVoid(object: VariablesScopeTracker() {

                override fun visitExpression(expression: IrExpression) {
                    val suspensionPoint = expression as? IrSuspensionPoint
                    if (suspensionPoint == null) {
                        super.visitExpression(expression)
                        return
                    }

                    suspensionPoint.result.acceptChildrenVoid(this)
                    suspensionPoint.resumeResult.acceptChildrenVoid(this)

                    val visibleVariables = mutableListOf<IrVariable>()
                    scopeStack.forEach { visibleVariables += it }
                    result.put(suspensionPoint, visibleVariables)
                }
            })

            return result
        }

        // These are marker descriptors to split up the lowering on two parts.
        private val saveState = IrFunctionImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, IrDeclarationOrigin.DEFINED,
                SimpleFunctionDescriptorImpl.create(
                        irFunction.descriptor,
                        Annotations.EMPTY,
                        "saveState".synthesizedName,
                        CallableMemberDescriptor.Kind.SYNTHESIZED,
                        SourceElement.NO_SOURCE).apply {
                    initialize(null, null, emptyList(), emptyList(), context.builtIns.unitType, Modality.ABSTRACT, Visibilities.PRIVATE)
                }).apply {
            returnType = context.irBuiltIns.unitType
        }

        private val restoreState = IrFunctionImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, IrDeclarationOrigin.DEFINED,
                SimpleFunctionDescriptorImpl.create(
                        irFunction.descriptor,
                        Annotations.EMPTY,
                        "restoreState".synthesizedName,
                        CallableMemberDescriptor.Kind.SYNTHESIZED,
                        SourceElement.NO_SOURCE).apply {
                    initialize(null, null, emptyList(), emptyList(), context.builtIns.unitType, Modality.ABSTRACT, Visibilities.PRIVATE)
                }).apply {
            returnType = context.irBuiltIns.unitType
        }

        private inner class ExpressionSlicer(val suspensionPointIdType: IrType): IrElementTransformerVoid() {
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
                            val tmp = irVar(transformedChild)

                            tempStatements += tmp
                            newChildren[index] = irGet(tmp)
                        }
                    }

                    var calledSaveState = false
                    var suspendCall: IrExpression? = null
                    when {
                        expression.isReturnIfSuspendedCall -> {
                            calledSaveState = true
                            val firstArgument = newChildren[2]!!
                            newChildren[2] = irBlock(firstArgument) {
                                +irCall(saveState)
                                +firstArgument
                            }
                            suspendCall = newChildren[2]
                        }
                        expression.isSuspendCall -> {
                            val lastChild = newChildren.last()
                            if (lastChild != null) {
                                // Save state as late as possible.
                                calledSaveState = true
                                newChildren[numberOfChildren - 1] =
                                        irBlock(lastChild) {
                                            if (lastChild.isPure()) {
                                                +irCall(saveState)
                                                +lastChild
                                            } else {
                                                val tmp = irVar(lastChild)
                                                +tmp
                                                +irCall(saveState)
                                                +irGet(tmp)
                                            }
                                        }
                            }
                            suspendCall = expression
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

                    if (suspendCall == null)
                        return irWrap(expression, tempStatements)

                    val suspensionPointIdParameter = IrTemporaryVariableDescriptorImpl(
                            containingDeclaration = irFunction.descriptor,
                            name                  = "suspensionPointId${suspensionPointIdIndex++}".synthesizedName,
                            outType               = suspensionPointIdType.toKotlinType())
                    val suspensionPoint = IrSuspensionPointImpl(
                            startOffset                = startOffset,
                            endOffset                  = endOffset,
                            type                       = context.irBuiltIns.anyNType,
                            suspensionPointIdParameter = irVar(suspensionPointIdParameter, suspensionPointIdType),
                            result                     = irBlock(startOffset, endOffset) {
                                if (!calledSaveState)
                                    +irCall(saveState)
                                +irSetVar(suspendResult.symbol, suspendCall)
                                +irReturnIfSuspended(suspendResult)
                                +irGet(suspendResult)
                            },
                            resumeResult               = irBlock(startOffset, endOffset) {
                                +irCall(restoreState)
                                +irThrowIfNotNull(exceptionArgument)
                                +irGet(dataArgument)
                            })
                    val expressionResult = when {
                        suspendCall.type.isUnit() -> irImplicitCoercionToUnit(suspensionPoint)
                        else -> irAs(suspensionPoint, suspendCall.type)
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

                    override fun visitExpression(expression: IrExpression) {
                        expression.acceptChildrenVoid(this)
                        hasSuspendCalls = hasSuspendCalls || expression is IrSuspensionPointImpl
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

        private fun IrBuilderWithScope.irVar(initializer: IrExpression) =
                irVar(
                        IrTemporaryVariableDescriptorImpl(
                                containingDeclaration = irFunction.descriptor,
                                name = "tmp${tempIndex++}".synthesizedName,
                                outType = initializer.type.toKotlinType()
                        ),
                        initializer.type
                ).apply { this.initializer = initializer }

        private fun IrBuilderWithScope.irVar(descriptor: VariableDescriptor, type: IrType) =
                IrVariableImpl(startOffset, endOffset, DECLARATION_ORIGIN_COROUTINE_IMPL, descriptor, type)

        private fun IrBuilderWithScope.irReturnIfSuspended(value: IrValueDeclaration) =
                irIfThen(irEqeqeq(irGet(value), irCall(symbols.coroutineSuspendedGetter)),
                        irReturn(irGet(value)))

        private fun IrBuilderWithScope.irThrowIfNotNull(exception: IrValueDeclaration) =
                irIfThen(irNot(irEqeqeq(irGet(exception), irNull())),
                        irThrow(irImplicitCast(irGet(exception), exception.type.makeNotNull())))

        fun IrBuilderWithScope.irDebugOutput(value: IrExpression) =
                irCall(symbols.println).apply {
                    putValueArgument(0, irCall(symbols.anyNToString).apply {
                        extensionReceiver = value
                    })
                }
    }

    private open class VariablesScopeTracker: IrElementVisitorVoid {

        protected val scopeStack = mutableListOf<MutableSet<IrVariable>>(mutableSetOf())

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
            scopeStack.peek()!!.add(declaration)
        }
    }
}