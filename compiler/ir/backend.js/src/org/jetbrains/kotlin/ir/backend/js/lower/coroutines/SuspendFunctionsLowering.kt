/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.coroutines

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.descriptors.getFunction
import org.jetbrains.kotlin.backend.common.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.common.ir.createOverriddenDescriptor
import org.jetbrains.kotlin.backend.common.lower.SymbolWithIrBuilder
import org.jetbrains.kotlin.backend.common.lower.copyAsValueParameter
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ClassConstructorDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.*
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrConstructorImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptorImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.utils.DFS

internal class SuspendFunctionsLowering(val context: JsIrBackendContext): FileLoweringPass {

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
                    ?: throw Error("Non-local callable reference to suspend lambda: $descriptor")
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
                    && suspendCallAtEnd -> SuspendFunctionKind.DELEGATING(
                lastCall!!
            )
            else                        -> SuspendFunctionKind.NEEDS_STATE_MACHINE
        }
    }

    private val symbols = context.ir.symbols
    private val unit = context.run { symbolTable.referenceClass(builtIns.unit) }
    private val getContinuationSymbol =
        context.run {
            val f = getInternalFunctions("getContinuation")
            symbolTable.referenceSimpleFunction(f.single())
        }
    private val continuationClassSymbol = getContinuationSymbol.owner.returnType.classifierOrFail as IrClassSymbol
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
                                        irCall(getContinuationSymbol, getContinuationSymbol.owner.returnType, listOf(irFunction.returnType)))
                            }
                            putValueArgument(0, irGetObject(unit)) // value
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

    private inner class CoroutineBuilder(val irFunction: IrFunction, val functionReference: IrFunctionReference?) {

        private val functionParameters = irFunction.explicitParameters
        private val boundFunctionParameters = functionReference?.getArgumentsWithIr()?.map { it.first }
        private val unboundFunctionParameters = boundFunctionParameters?.let { functionParameters - it }

        private lateinit var suspendResult: IrVariable
        private lateinit var suspendState: IrVariable
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

        private val coroutineImplLabelFieldSymbol = coroutineImplSymbol.getPropertyField("label")!!
//        private val coroutineImplResultFieldSymbol = coroutineImplSymbol.getPropertyField("pendingResult")!!
        private val coroutineImplExceptionFieldSymbol = coroutineImplSymbol.getPropertyField("pendingException")!!
        private val coroutineImplExceptionStateFieldSymbol = coroutineImplSymbol.getPropertyField("exceptionState")!!

        private val coroutineConstructors = mutableListOf<IrConstructor>()
        private var exceptionTrapId = -1

        fun build(): BuiltCoroutine {
            val superTypes = mutableListOf<IrType>(coroutineImplSymbol.owner.defaultType)
            var suspendFunctionClass: IrClass? = null
            var functionClass: IrClass? = null
            var suspendFunctionClassTypeArguments: List<IrType>? = null
            var functionClassTypeArguments: List<IrType>? = null
            if (unboundFunctionParameters != null) {
                // Suspend lambda inherits SuspendFunction.
                val numberOfParameters = unboundFunctionParameters.size
                suspendFunctionClass = context.suspendFunctions[numberOfParameters].owner
                val unboundParameterTypes = unboundFunctionParameters.map { it.type }
                suspendFunctionClassTypeArguments = unboundParameterTypes + irFunction.returnType
                superTypes += suspendFunctionClass.typeWith(suspendFunctionClassTypeArguments)

                functionClass = context.functions[numberOfParameters + 1].owner
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
            val constructors = mutableSetOf<ClassConstructorDescriptor>()
            val coroutineConstructorBuilder = createConstructorBuilder()
            constructors.add(coroutineConstructorBuilder.symbol.descriptor)
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
                constructors.add(coroutineFactoryConstructorBuilder.symbol.descriptor)

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
            coroutineConstructors += coroutineConstructorBuilder.ir

            coroutineFactoryConstructorBuilder?.let {
                it.initialize()
                coroutineClass.addChild(it.ir)
                coroutineConstructors += it.ir
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

            setupExceptionState()

            return BuiltCoroutine(
                coroutineClass = coroutineClass,
                coroutineConstructor = coroutineFactoryConstructorBuilder?.ir
                    ?: coroutineConstructorBuilder.ir,
                doResumeFunction = doResumeMethodBuilder.ir
            )
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
                                                irCall(getContinuationSymbol, getContinuationSymbol.owner.returnType, listOf(returnType)))
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
                suspendResult = JsIrBuilder.buildVar(IrVariableSymbolImpl(
                    IrTemporaryVariableDescriptorImpl(
                        containingDeclaration = irFunction.descriptor,
                        name = "suspendResult".synthesizedName,
                        outType = context.builtIns.nullableAnyType,
                        isMutable = true
                    )
                ), JsIrBuilder.buildGetValue(dataArgument.symbol), context.irBuiltIns.anyNType)
                suspendState = JsIrBuilder.buildVar(IrVariableSymbolImpl(
                    IrTemporaryVariableDescriptorImpl(
                        containingDeclaration = irFunction.descriptor,
                        name = "suspendState".synthesizedName,
                        outType = coroutineImplLabelFieldSymbol.owner.type.toKotlinType(),
                        isMutable = true
                    )
                ), type = coroutineImplLabelFieldSymbol.owner.type)

                val body =
                    (originalBody as IrBlockBody).run {
                        IrBlockImpl(
                            startOffset,
                            endOffset,
                            context.irBuiltIns.unitType,
                            STATEMENT_ORIGIN_COROUTINE_IMPL,
                            statements
                        )
                    }

                buildStateMachine(body, function)

                return function
            }
        }

        private fun setupExceptionState() {
            for (it in coroutineConstructors) {
                (it.body as? IrBlockBody)?.run {
                    val receiver = JsIrBuilder.buildGetValue(coroutineClassThis.symbol)
                    val id = JsIrBuilder.buildInt(context.irBuiltIns.intType, exceptionTrapId)
                    statements += JsIrBuilder.buildSetField(coroutineImplExceptionStateFieldSymbol, receiver, id, context.irBuiltIns.unitType)
                }
            }
        }

        private fun buildStateMachine(body: IrBlock, function: IrFunction) {
            val unit = context.irBuiltIns.unitType

            val switch = IrWhenImpl(body.startOffset, body.endOffset, unit,
                                    COROUTINE_SWITCH
            )
            val rootTry = IrTryImpl(body.startOffset, body.endOffset, unit).apply {
                tryResult = switch
            }
            val rootLoop = IrDoWhileLoopImpl(
                body.startOffset,
                body.endOffset,
                unit,
                COROUTINE_ROOT_LOOP,
                rootTry,
                JsIrBuilder.buildBoolean(context.irBuiltIns.booleanType, true)
            )

            val suspendableNodes = mutableSetOf<IrElement>()
            val loweredBody =
                collectSuspendableNodes(body, suspendableNodes, context, function)
            val thisReceiver = (function.dispatchReceiverParameter as IrValueParameter).symbol

            val stateMachineBuilder = StateMachineBuilder(
                suspendableNodes,
                context,
                function.symbol,
                rootLoop,
                coroutineImplExceptionFieldSymbol,
                coroutineImplExceptionStateFieldSymbol,
                coroutineImplLabelFieldSymbol,
                thisReceiver,
                suspendResult.symbol
            )

            loweredBody.acceptVoid(stateMachineBuilder)

            stateMachineBuilder.finalizeStateMachine()

            rootTry.catches += stateMachineBuilder.globalCatch

            val visited = mutableSetOf<SuspendState>()

            val sortedStates = DFS.topologicalOrder(listOf(stateMachineBuilder.entryState), { it.successors }, { visited.add(it) })
            sortedStates.withIndex().forEach { it.value.id = it.index }

            fun buildDispatch(target: SuspendState) = target.run {
                assert(id >= 0)
                JsIrBuilder.buildInt(context.irBuiltIns.intType, id)
            }

            val eqeqeqInt = context.irBuiltIns.eqeqeqSymbol

            for (state in sortedStates) {
                val condition = JsIrBuilder.buildCall(eqeqeqInt).apply {
                    putValueArgument(0, JsIrBuilder.buildGetField(coroutineImplLabelFieldSymbol, JsIrBuilder.buildGetValue(thisReceiver)))
                    putValueArgument(1, JsIrBuilder.buildInt(context.irBuiltIns.intType, state.id))
                }

                switch.branches += IrBranchImpl(state.entryBlock.startOffset, state.entryBlock.endOffset, condition, state.entryBlock)
            }

            val irResultDeclaration = suspendResult

            rootLoop.transform(DispatchPointTransformer(::buildDispatch), null)

            exceptionTrapId = stateMachineBuilder.rootExceptionTrap.id

            val functionBody = IrBlockBodyImpl(function.startOffset, function.endOffset, listOf(irResultDeclaration, rootLoop))

            function.body = functionBody

            val liveLocals = computeLivenessAtSuspensionPoints(functionBody).values.flatten().toSet()

            val localToPropertyMap = mutableMapOf<IrValueSymbol, IrFieldSymbol>()
            var localCounter = 0
            // TODO: optimize by using the same property for different locals.
            liveLocals.forEach {
                if (it != suspendState && it != suspendResult) {
                    localToPropertyMap.getOrPut(it.symbol) {
                        addField(Name.identifier("${it.name}${localCounter++}"), it.type, (it as? IrVariable)?.isVar ?: false).symbol
                    }
                }
            }
            irFunction.explicitParameters.forEach {
                localToPropertyMap.getOrPut(it.symbol) {
                    argumentToPropertiesMap.getValue(it.descriptor).symbol
                }
            }

            function.transform(
                LiveLocalsTransformer(
                    localToPropertyMap,
                    JsIrBuilder.buildGetValue(
                        thisReceiver
                    ),
                    unit
                ), null)
        }

        private fun computeLivenessAtSuspensionPoints(body: IrBody): Map<IrCall, List<IrValueDeclaration>> {
            // TODO: data flow analysis.
            // Just save all visible for now.
            val result = mutableMapOf<IrCall, List<IrValueDeclaration>>()
            body.acceptChildrenVoid(object : VariablesScopeTracker() {
                override fun visitCall(expression: IrCall) {
                    if (!expression.descriptor.isSuspend) return super.visitCall(expression)

                    expression.acceptChildrenVoid(this)
                    val visibleVariables = mutableListOf<IrValueDeclaration>()
                    scopeStack.forEach { visibleVariables += it }
                    result.put(expression, visibleVariables)
                }
            })

            return result
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