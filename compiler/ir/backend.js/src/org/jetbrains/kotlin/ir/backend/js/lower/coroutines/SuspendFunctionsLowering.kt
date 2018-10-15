/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.coroutines

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.descriptors.*
import org.jetbrains.kotlin.backend.common.ir.copyParameterDeclarationsFrom
import org.jetbrains.kotlin.backend.common.ir.isSuspend
import org.jetbrains.kotlin.backend.common.lower.SymbolWithIrBuilder
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.ir.addChild
import org.jetbrains.kotlin.ir.backend.js.ir.simpleFunctions
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrConstructorImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.DFS

internal class SuspendFunctionsLowering(val context: JsIrBackendContext): FileLoweringPass {

    private object STATEMENT_ORIGIN_COROUTINE_IMPL : IrStatementOriginImpl("COROUTINE_IMPL")
    private object DECLARATION_ORIGIN_COROUTINE_IMPL : IrDeclarationOriginImpl("COROUTINE_IMPL")

    private val builtCoroutines = mutableMapOf<IrFunction, BuiltCoroutine>()
    private val suspendLambdas = mutableMapOf<IrFunction, IrFunctionReference>()

    override fun lower(irFile: IrFile) {
        markSuspendLambdas(irFile)
        buildCoroutines(irFile)
        transformCallableReferencesToSuspendLambdas(irFile)
    }

    private fun buildCoroutines(irFile: IrFile) {
        irFile.declarations.transformFlat(::tryTransformSuspendFunction)
        irFile.acceptVoid(object : IrElementVisitorVoid {
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
        if (element is IrSimpleFunction && element.isSuspend && element.modality != Modality.ABSTRACT)
            transformSuspendFunction(element, suspendLambdas[element])
        else null

    private fun markSuspendLambdas(irElement: IrElement) {
        irElement.acceptChildrenVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitFunctionReference(expression: IrFunctionReference) {
                expression.acceptChildrenVoid(this)

                if (expression.isSuspend) {
                    suspendLambdas[expression.symbol.owner] = expression
                }
            }
        })
    }

    private fun transformCallableReferencesToSuspendLambdas(irElement: IrElement) {
        irElement.transformChildrenVoid(object : IrElementTransformerVoid() {

            override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
                expression.transformChildrenVoid(this)

                if (!expression.isSuspend)
                    return expression
                val coroutine = builtCoroutines[expression.symbol.owner]
                    ?: throw Error("Non-local callable reference to suspend lambda: $expression")
                val constructorParameters = coroutine.coroutineConstructor.valueParameters
                val expressionArguments = expression.getArguments().map { it.second }
                assert(constructorParameters.size == expressionArguments.size) {
                    "Inconsistency between callable reference to suspend lambda and the corresponding coroutine"
                }
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

    private fun transformSuspendFunction(irFunction: IrSimpleFunction, functionReference: IrFunctionReference?): List<IrDeclaration>? {
        val suspendFunctionKind = getSuspendFunctionKind(irFunction)
        return when (suspendFunctionKind) {
            is SuspendFunctionKind.NO_SUSPEND_CALLS -> {
                null                                                            // No suspend function calls - just an ordinary function.
            }

            is SuspendFunctionKind.DELEGATING -> {                              // Calls another suspend function at the end.
                removeReturnIfSuspendedCallAndSimplifyDelegatingCall(irFunction, suspendFunctionKind.delegatingCall)
                null                                                            // No need in state machine.
            }

            is SuspendFunctionKind.NEEDS_STATE_MACHINE -> {
                val coroutine = buildCoroutine(irFunction, functionReference)   // Coroutine implementation.
                if (irFunction in suspendLambdas)             // Suspend lambdas are called through factory method <create>,
                    listOf(coroutine)                                           // thus we can eliminate original body.
                else
                    listOf<IrDeclaration>(coroutine, irFunction)
            }
        }
    }

    private fun getSuspendFunctionKind(irFunction: IrSimpleFunction): SuspendFunctionKind {
        if (irFunction in suspendLambdas)
            return SuspendFunctionKind.NEEDS_STATE_MACHINE            // Suspend lambdas always need coroutine implementation.

        val body = irFunction.body ?: return SuspendFunctionKind.NO_SUSPEND_CALLS

        var numberOfSuspendCalls = 0
        body.acceptVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitCall(expression: IrCall) {
                expression.acceptChildrenVoid(this)
                if (expression.isSuspend)
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
                loop@ while (true) {
                    value = when {
                        value is IrBlock && value.statements.size == 1 -> value.statements.first()
                        value is IrReturn -> value.value
                        else -> break@loop
                    }
                }
                value as? IrCall
            }
            else -> null
        }

        val suspendCallAtEnd = lastCall != null && lastCall.isSuspend    // Suspend call.
        return when {
            numberOfSuspendCalls == 0 -> SuspendFunctionKind.NO_SUSPEND_CALLS
            numberOfSuspendCalls == 1
                    && suspendCallAtEnd -> SuspendFunctionKind.DELEGATING(lastCall!!)
            else -> SuspendFunctionKind.NEEDS_STATE_MACHINE
        }
    }

    private val symbols = context.ir.symbols
    private val unit = context.run { symbolTable.referenceClass(builtIns.unit) }
    private val getContinuationSymbol = context.intrinsics.getContinuation
    private val continuationClassSymbol = getContinuationSymbol.owner.returnType.classifierOrFail as IrClassSymbol
    private val returnIfSuspended = context.intrinsics.returnIfSuspended

    private fun removeReturnIfSuspendedCallAndSimplifyDelegatingCall(irFunction: IrFunction, delegatingCall: IrCall) {
        val returnValue =
            if (delegatingCall.descriptor.original == returnIfSuspended.descriptor)
                delegatingCall.getValueArgument(0)!!
            else delegatingCall
        context.createIrBuilder(irFunction.symbol).run {
            val statements = (irFunction.body as IrBlockBody).statements
            val lastStatement = statements.last()
            assert(lastStatement == delegatingCall || lastStatement is IrReturn) { "Unexpected statement $lastStatement" }
            statements[statements.lastIndex] = irReturn(returnValue)
        }
    }

    private fun buildCoroutine(irFunction: IrSimpleFunction, functionReference: IrFunctionReference?): IrClass {
        val coroutine = CoroutineBuilder(irFunction, functionReference).build()
        builtCoroutines[irFunction] = coroutine

        if (functionReference == null) {
            val resultSetter = context.coroutineImplResultSymbol.setter!!
            val exceptionSetter = context.coroutineImplExceptionProperty.setter!!
            // It is not a lambda - replace original function with a call to constructor of the built coroutine.
            val irBuilder = context.createIrBuilder(irFunction.symbol, irFunction.startOffset, irFunction.endOffset)
            irFunction.body = irBuilder.irBlockBody(irFunction) {
                val dispatchReceiverCall = irCall(coroutine.coroutineConstructor.symbol).apply {
                    val functionParameters = irFunction.explicitParameters
                    functionParameters.forEachIndexed { index, argument ->
                        putValueArgument(index, irGet(argument))
                    }
                    putValueArgument(
                        functionParameters.size,
                        irCall(getContinuationSymbol, getContinuationSymbol.owner.returnType, listOf(irFunction.returnType))
                    )
                }
                val dispatchReceiverVar = JsIrBuilder.buildVar(dispatchReceiverCall.type, irFunction, initializer = dispatchReceiverCall)
                +dispatchReceiverVar
                +irCall(resultSetter).apply {
                    dispatchReceiver = irGet(dispatchReceiverVar)
                    putValueArgument(0, irGetObject(unit))
                }
                +irCall(exceptionSetter).apply {
                    dispatchReceiver = irGet(dispatchReceiverVar)
                    putValueArgument(0, irNull())
                }
                +irReturn(irCall(coroutine.doResumeFunction.symbol).apply {
                    dispatchReceiver = irGet(dispatchReceiverVar)
                })
            }
        }

        return coroutine.coroutineClass
    }

    private class BuiltCoroutine(
        val coroutineClass: IrClass,
        val coroutineConstructor: IrConstructor,
        val doResumeFunction: IrFunction
    )

    private var coroutineId = 0

    private inner class CoroutineBuilder(val irFunction: IrFunction, val functionReference: IrFunctionReference?) {

        private val functionParameters = irFunction.explicitParameters
        private val boundFunctionParameters = functionReference?.getArgumentsWithIr()?.map { it.first }
        private val unboundFunctionParameters = boundFunctionParameters?.let { functionParameters - it }

        private lateinit var suspendResult: IrVariable
        private lateinit var suspendState: IrVariable
        private lateinit var coroutineClass: IrClassImpl
        private lateinit var coroutineClassThis: IrValueParameter
        private lateinit var argumentToPropertiesMap: Map<IrValueParameter, IrField>

        private val coroutineImplSymbol = symbols.coroutineImpl
        private val coroutineImplConstructorSymbol = coroutineImplSymbol.constructors.single()
        private val create1Function = coroutineImplSymbol.owner.simpleFunctions()
            .single { it.name.asString() == "create" && it.valueParameters.size == 1 }

        private val create1CompletionParameter = create1Function.valueParameters[0]

        private val coroutineImplLabelProperty = context.coroutineImplLabelProperty
        private val coroutineImplResultSymbol = context.coroutineImplResultSymbol
        private val coroutineImplExceptionProperty = context.coroutineImplExceptionProperty
        private val coroutineImplExceptionStateProperty = context.coroutineImplExceptionStateProperty

        private val coroutineConstructors = mutableListOf<IrConstructor>()
        private var exceptionTrapId = -1

        fun build(): BuiltCoroutine {
            val superTypes = mutableListOf(coroutineImplSymbol.owner.defaultType)
            var suspendFunctionClass: IrClass? = null
            var functionClass: IrClass? = null
            val suspendFunctionClassTypeArguments: List<IrType>?
            val functionClassTypeArguments: List<IrType>?
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

            val coroutineClassDescriptor = WrappedClassDescriptor()
            val coroutineClassSymbol = IrClassSymbolImpl(coroutineClassDescriptor)

            coroutineClass = IrClassImpl(
                irFunction.startOffset,
                irFunction.endOffset,
                DECLARATION_ORIGIN_COROUTINE_IMPL,
                coroutineClassSymbol,
                "${irFunction.name}\$${coroutineId++}".synthesizedName,
                ClassKind.CLASS,
                irFunction.visibility,
                Modality.FINAL,
                false,
                false,
                false,
                false,
                false
            )
            coroutineClassDescriptor.bind(coroutineClass)

            coroutineClass.parent = irFunction.parent
            coroutineClass.superTypes += superTypes
            val thisType = IrSimpleTypeImpl(coroutineClassSymbol, false, emptyList(), emptyList())
            coroutineClassThis =
                    JsIrBuilder.buildValueParameter(Name.special("<this>"), -1, thisType, IrDeclarationOrigin.INSTANCE_RECEIVER)
            coroutineClass.thisReceiver = coroutineClassThis

            val overriddenMap = mutableMapOf<IrSimpleFunction, IrSimpleFunctionSymbol>()
            val constructors = mutableSetOf<IrConstructor>()
            val coroutineConstructorBuilder = createConstructorBuilder()
            coroutineConstructorBuilder.initialize()
            constructors.add(coroutineConstructorBuilder.ir)

            val doResumeFunction = coroutineImplSymbol.owner.simpleFunctions().single { it.name.asString() == "doResume" }
            val doResumeMethodBuilder = createDoResumeMethodBuilder(doResumeFunction, coroutineClass)
            doResumeMethodBuilder.initialize()
            overriddenMap += doResumeFunction to doResumeMethodBuilder.symbol

            var coroutineFactoryConstructorBuilder: SymbolWithIrBuilder<IrConstructorSymbol, IrConstructor>? = null
            var createMethodBuilder: SymbolWithIrBuilder<IrSimpleFunctionSymbol, IrSimpleFunction>? = null
            var invokeMethodBuilder: SymbolWithIrBuilder<IrSimpleFunctionSymbol, IrSimpleFunction>? = null
            if (functionReference != null) {
                // Suspend lambda - create factory methods.
                coroutineFactoryConstructorBuilder = createFactoryConstructorBuilder(boundFunctionParameters!!).also { it.initialize() }
                constructors.add(coroutineFactoryConstructorBuilder.ir)

                val createFunctionDeclaration = coroutineImplSymbol.owner.simpleFunctions()
                    .asSequence()
                    .filter { it.name == Name.identifier("create") }
                    .toList()
                    .atMostOne { it.valueParameters.size == unboundFunctionParameters!!.size + 1 }

                createMethodBuilder = createCreateMethodBuilder(
                    unboundArgs                    = unboundFunctionParameters!!,
                    superFunctionDeclaration       = createFunctionDeclaration,
                    coroutineConstructor           = coroutineConstructorBuilder.ir,
                    coroutineClass                 = coroutineClass)
                createMethodBuilder.initialize()

                if (createFunctionDeclaration != null)
                    overriddenMap += createFunctionDeclaration to createMethodBuilder.symbol

                val invokeFunctionDeclaration = functionClass!!.simpleFunctions()
                    .atMostOne { it.name == Name.identifier("invoke") }!!
                val suspendInvokeFunctionDeclaration = suspendFunctionClass!!.simpleFunctions()
                    .atMostOne { it.name == Name.identifier("invoke") }!!
                invokeMethodBuilder = createInvokeMethodBuilder(
                    suspendFunctionInvokeFunctionDeclaration = suspendInvokeFunctionDeclaration,
                    functionInvokeFunctionDeclaration = invokeFunctionDeclaration,
                    createFunction = createMethodBuilder.ir,
                    doResumeFunction = doResumeMethodBuilder.ir,
                    coroutineClass = coroutineClass
                )
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
                coroutineConstructor = coroutineFactoryConstructorBuilder?.ir ?: coroutineConstructorBuilder.ir,
                doResumeFunction = doResumeMethodBuilder.ir
            )
        }

        fun IrClass.setSuperSymbolsAndAddFakeOverrides(superTypes: List<IrType>) {

            fun IrDeclaration.toList() = when (this) {
                is IrSimpleFunction -> listOf(this)
//                is IrProperty -> listOfNotNull(getter, setter)
                else -> emptyList()
            }

            val overriddenMembers = declarations.flatMap { it.toList() }.flatMap { it.overriddenSymbols.map(IrSimpleFunctionSymbol::owner) }

            val unoverriddenSuperMembers = superTypes.map { it.getClass()!! }.flatMap { irClass ->
                irClass.declarations.flatMap { it.toList() }.filter { it !in overriddenMembers }
            }

            fun createFakeOverride(irFunction: IrSimpleFunction) = JsIrBuilder.buildFunction(
                irFunction.name,
                irFunction.visibility,
                Modality.FINAL,
                irFunction.isInline,
                irFunction.isExternal,
                irFunction.isTailrec,
                irFunction.isSuspend,
                IrDeclarationOrigin.FAKE_OVERRIDE
            ).apply {
                returnType = irFunction.returnType
                overriddenSymbols += irFunction.symbol
                copyParameterDeclarationsFrom(irFunction)
            }

            for (sm in unoverriddenSuperMembers) {
                val fakeOverride = createFakeOverride(sm).also { it.parent = this }
                declarations += fakeOverride
            }

            /*

    return when (descriptor) {
        is FunctionDescriptor -> descriptor.createFunction()
        is PropertyDescriptor ->
            IrPropertyImpl(startOffset, endOffset, IrDeclarationOrigin.FAKE_OVERRIDE, descriptor).apply {
                // TODO: add field if getter is missing?
                getter = descriptor.getter?.createFunction() as IrSimpleFunction?
                setter = descriptor.setter?.createFunction() as IrSimpleFunction?
            }
        else -> TODO(descriptor.toString())
    }
             */

        }

        private fun createConstructorBuilder() = object : SymbolWithIrBuilder<IrConstructorSymbol, IrConstructor>() {

            private val descriptor = WrappedClassConstructorDescriptor()

            override fun buildSymbol(): IrConstructorSymbol = IrConstructorSymbolImpl(descriptor)

            override fun doInitialize() {}

            override fun buildIr(): IrConstructor {
                // Save all arguments to fields.
                argumentToPropertiesMap = functionParameters.associate {
                    it to addField(it.name, it.type, false)
                }

                val completion = coroutineImplConstructorSymbol.owner.valueParameters[0]

                val declaration = IrConstructorImpl(
                    irFunction.startOffset,
                    irFunction.endOffset,
                    DECLARATION_ORIGIN_COROUTINE_IMPL,
                    symbol,
                    Name.special("<init>"),
                    irFunction.visibility,
                    false,
                    false,
                    false
                )

                descriptor.bind(declaration)
                declaration.parent = coroutineClass
                declaration.returnType = coroutineClass.defaultType

                declaration.valueParameters += functionParameters.map {
                    JsIrBuilder.buildValueParameter(it.name, it.index, it.type, it.origin).also { p -> p.parent = declaration }
                }
                declaration.valueParameters += JsIrBuilder.buildValueParameter(
                    completion.name,
                    functionParameters.size,
                    completion.type,
                    completion.origin
                ).also {
                    it.parent = declaration
                }

                val irBuilder = context.createIrBuilder(symbol, irFunction.startOffset, irFunction.endOffset)

                declaration.body = irBuilder.irBlockBody {
                    val completionParameter = declaration.valueParameters.last()
                    +IrDelegatingConstructorCallImpl(
                        irFunction.startOffset, irFunction.endOffset,
                        context.irBuiltIns.unitType,
                        coroutineImplConstructorSymbol, coroutineImplConstructorSymbol.descriptor
                    ).apply {
                        putValueArgument(0, irGet(completionParameter))
                    }
                    +IrInstanceInitializerCallImpl(
                        irFunction.startOffset,
                        irFunction.endOffset,
                        coroutineClass.symbol,
                        context.irBuiltIns.unitType
                    )
                    functionParameters.forEachIndexed { index, parameter ->
                        +irSetField(
                            irGet(coroutineClassThis),
                            argumentToPropertiesMap[parameter]!!,
                            irGet(declaration.valueParameters[index])
                        )
                    }
                }

                return declaration
            }
        }

        private fun createFactoryConstructorBuilder(boundParams: List<IrValueParameter>) =
            object : SymbolWithIrBuilder<IrConstructorSymbol, IrConstructor>() {

                private val descriptor = WrappedClassConstructorDescriptor()

                override fun buildSymbol() = IrConstructorSymbolImpl(descriptor)

                override fun doInitialize() {}

                override fun buildIr(): IrConstructor {

                    val declaration = IrConstructorImpl(
                        irFunction.startOffset,
                        irFunction.endOffset,
                        DECLARATION_ORIGIN_COROUTINE_IMPL,
                        symbol,
                        Name.special("<init>"),
                        irFunction.visibility,
                        false,
                        false,
                        false
                    )

                    descriptor.bind(declaration)
                    declaration.parent = coroutineClass
                    declaration.returnType = coroutineClass.defaultType

                    boundParams.mapIndexedTo(declaration.valueParameters) { i, p ->
                        JsIrBuilder.buildValueParameter(p.name, i, p.type, p.origin).also { it.parent = declaration }
                    }

                    val irBuilder = context.createIrBuilder(symbol, irFunction.startOffset, irFunction.endOffset)
                    declaration.body = irBuilder.irBlockBody {
                        +IrDelegatingConstructorCallImpl(
                            irFunction.startOffset, irFunction.endOffset, context.irBuiltIns.unitType,
                            coroutineImplConstructorSymbol, coroutineImplConstructorSymbol.descriptor
                        ).apply {
                            putValueArgument(0, irNull()) // Completion.
                        }
                        +IrInstanceInitializerCallImpl(
                            irFunction.startOffset, irFunction.endOffset, coroutineClass.symbol,
                            context.irBuiltIns.unitType
                        )
                        // Save all arguments to fields.
                        boundParams.forEachIndexed { index, parameter ->
                            +irSetField(
                                irGet(coroutineClassThis), argumentToPropertiesMap[parameter]!!,
                                irGet(declaration.valueParameters[index])
                            )
                        }
                    }

                    return declaration
                }
            }

        private fun createCreateMethodBuilder(
            unboundArgs: List<IrValueParameter>,
            superFunctionDeclaration: IrSimpleFunction?,
            coroutineConstructor: IrConstructor,
            coroutineClass: IrClass
        ) = object : SymbolWithIrBuilder<IrSimpleFunctionSymbol, IrSimpleFunction>() {

            private val descriptor = WrappedSimpleFunctionDescriptor()

            override fun buildSymbol() = IrSimpleFunctionSymbolImpl(descriptor)

            override fun doInitialize() {}

            override fun buildIr(): IrSimpleFunction {
                val declaration = IrFunctionImpl(
                    irFunction.startOffset,
                    irFunction.endOffset,
                    DECLARATION_ORIGIN_COROUTINE_IMPL,
                    symbol,
                    Name.identifier("create"),
                    Visibilities.PRIVATE,
                    Modality.FINAL,
                    false,
                    false,
                    false,
                    false
                )

                descriptor.bind(declaration)
                declaration.parent = coroutineClass
                declaration.returnType = coroutineClass.defaultType
                declaration.dispatchReceiverParameter = coroutineClassThis

                unboundArgs.mapIndexedTo(declaration.valueParameters) { i, p ->
                    JsIrBuilder.buildValueParameter(p.name, i, p.type, p.origin).also { it.parent = declaration }
                }

                declaration.valueParameters += JsIrBuilder.buildValueParameter(
                    create1CompletionParameter.name,
                    unboundArgs.size,
                    create1CompletionParameter.type,
                    create1CompletionParameter.origin
                ).also { it.parent = declaration }


                if (superFunctionDeclaration != null) {
                    declaration.overriddenSymbols += superFunctionDeclaration.overriddenSymbols
                    declaration.overriddenSymbols += superFunctionDeclaration.symbol
                }

                val thisReceiver = coroutineClassThis
                val irBuilder = context.createIrBuilder(symbol, irFunction.startOffset, irFunction.endOffset)
                declaration.body = irBuilder.irBlockBody(irFunction.startOffset, irFunction.endOffset) {
                    +irReturn(
                        irCall(coroutineConstructor).apply {
                            var unboundIndex = 0
                            val unboundArgsSet = unboundArgs.toSet()
                            functionParameters.map {
                                if (unboundArgsSet.contains(it))
                                    irGet(declaration.valueParameters[unboundIndex++])
                                else
                                    irGetField(irGet(thisReceiver), argumentToPropertiesMap[it]!!)
                            }.forEachIndexed { index, argument ->
                                putValueArgument(index, argument)
                            }
                            putValueArgument(functionParameters.size, irGet(declaration.valueParameters[unboundIndex]))
                            assert(unboundIndex == declaration.valueParameters.size - 1) { "Not all arguments of <create> are used" }
                        })
                }

                return declaration
            }
        }

        private fun createInvokeMethodBuilder(
            suspendFunctionInvokeFunctionDeclaration: IrSimpleFunction,
            functionInvokeFunctionDeclaration: IrSimpleFunction,
            createFunction: IrFunction,
            doResumeFunction: IrFunction,
            coroutineClass: IrClass
        ) = object : SymbolWithIrBuilder<IrSimpleFunctionSymbol, IrSimpleFunction>() {

            private val descriptor = WrappedSimpleFunctionDescriptor()

            override fun buildSymbol() = IrSimpleFunctionSymbolImpl(descriptor)

            override fun doInitialize() {}

            override fun buildIr(): IrSimpleFunction {

                val declaration = IrFunctionImpl(
                    irFunction.startOffset,
                    irFunction.endOffset,
                    DECLARATION_ORIGIN_COROUTINE_IMPL,
                    symbol,
                    Name.identifier("invoke"),
                    Visibilities.PRIVATE,
                    Modality.FINAL,
                    false,
                    false,
                    false,
                    true
                )

                descriptor.bind(declaration)
                declaration.parent = coroutineClass
                declaration.returnType = irFunction.returnType
                declaration.dispatchReceiverParameter = coroutineClassThis

                declaration.overriddenSymbols += suspendFunctionInvokeFunctionDeclaration.symbol
                declaration.overriddenSymbols += functionInvokeFunctionDeclaration.symbol

                createFunction.valueParameters.dropLast(1).mapTo(declaration.valueParameters) { p ->
                    JsIrBuilder.buildValueParameter(p.name, p.index, p.type, p.origin).also { it.parent = declaration }
                }

                val resultSetter = context.coroutineImplResultSymbol.setter!!
                val exceptionSetter = context.coroutineImplExceptionProperty.setter!!

                val thisReceiver = coroutineClassThis
                val irBuilder = context.createIrBuilder(symbol, irFunction.startOffset, irFunction.endOffset)
                declaration.body = irBuilder.irBlockBody(irFunction.startOffset, irFunction.endOffset) {
                    val dispatchReceiverCall = irCall(createFunction).apply {
                        dispatchReceiver = irGet(thisReceiver)
                        declaration.valueParameters.forEachIndexed { index, parameter ->
                            putValueArgument(index, irGet(parameter))
                        }
                        putValueArgument(
                            declaration.valueParameters.size,
                            irCall(getContinuationSymbol, getContinuationSymbol.owner.returnType, listOf(declaration.returnType))
                        )
                    }
                    val dispatchReceiverVar = JsIrBuilder.buildVar(dispatchReceiverCall.type, irFunction, initializer = dispatchReceiverCall)
                    +dispatchReceiverVar
                    +irCall(resultSetter).apply {
                        dispatchReceiver = irGet(dispatchReceiverVar)
                        putValueArgument(0, irGetObject(unit))
                    }
                    +irCall(exceptionSetter).apply {
                        dispatchReceiver = irGet(dispatchReceiverVar)
                        putValueArgument(0, irNull())
                    }
                    +irReturn(irCall(doResumeFunction).apply {
                        dispatchReceiver = irGet(dispatchReceiverVar)
                    })
                }

                return declaration
            }
        }

        private fun addField(name: Name, type: IrType, isMutable: Boolean): IrField {
            val descriptor = WrappedPropertyDescriptor()
            val symbol = IrFieldSymbolImpl(descriptor)
            return IrFieldImpl(
                irFunction.startOffset,
                irFunction.endOffset,
                DECLARATION_ORIGIN_COROUTINE_IMPL,
                symbol,
                name,
                type,
                Visibilities.PRIVATE,
                !isMutable,
                false,
                false
            ).also {
                descriptor.bind(it)
                it.parent = coroutineClass
                coroutineClass.addChild(it)
            }
        }

        private fun createDoResumeMethodBuilder(doResumeFunction: IrSimpleFunction, coroutineClass: IrClass) =
            object : SymbolWithIrBuilder<IrSimpleFunctionSymbol, IrSimpleFunction>() {

                private val descriptor = WrappedSimpleFunctionDescriptor()

                override fun buildSymbol() = IrSimpleFunctionSymbolImpl(descriptor)

                override fun doInitialize() {}

                override fun buildIr(): IrSimpleFunction {
                    val originalBody = irFunction.body!!
                    val function = IrFunctionImpl(
                        irFunction.startOffset, irFunction.endOffset, DECLARATION_ORIGIN_COROUTINE_IMPL, symbol,
                        doResumeFunction.name,
                        doResumeFunction.visibility,
                        Modality.FINAL,
                        doResumeFunction.isInline,
                        doResumeFunction.isExternal,
                        doResumeFunction.isTailrec,
                        doResumeFunction.isSuspend
                    )

                    descriptor.bind(function)
                    function.overriddenSymbols += doResumeFunction.symbol
                    function.parent = coroutineClass
                    function.returnType = context.irBuiltIns.anyNType
                    function.dispatchReceiverParameter = coroutineClassThis
                    doResumeFunction.valueParameters.mapTo(function.valueParameters) {
                        JsIrBuilder.buildValueParameter(it.name, it.index, it.type, it.origin).also { p -> p.parent = function }
                    }

                    val thisReceiver = JsIrBuilder.buildGetValue(coroutineClassThis.symbol)
                    suspendResult = JsIrBuilder.buildVar(
                        context.irBuiltIns.anyNType,
                        function,
                        "suspendResult",
                        true,
                        initializer = JsIrBuilder.buildCall(coroutineImplResultSymbol.getter!!.symbol).apply { dispatchReceiver = thisReceiver }
                    )

                    suspendState = JsIrBuilder.buildVar(coroutineImplLabelProperty.getter!!.returnType, function, "suspendState", true)

                    val body =
                        (originalBody as IrBlockBody).run {
                            IrBlockImpl(
                                irFunction.startOffset,
                                irFunction.endOffset,
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
                    statements += JsIrBuilder.buildCall(coroutineImplExceptionStateProperty.setter!!.symbol).also { call ->
                        call.dispatchReceiver = receiver
                        call.putValueArgument(0, id)
                    }
                }
            }
        }

        private fun buildStateMachine(body: IrBlock, function: IrFunction) {
            val unit = context.irBuiltIns.unitType

            val switch = IrWhenImpl(body.startOffset, body.endOffset, unit, COROUTINE_SWITCH)
            val rootTry = IrTryImpl(body.startOffset, body.endOffset, unit).apply { tryResult = switch }
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
                coroutineImplExceptionProperty,
                coroutineImplExceptionStateProperty,
                coroutineImplLabelProperty,
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
                    putValueArgument(0, JsIrBuilder.buildCall(coroutineImplLabelProperty.getter!!.symbol).also {
                        it.dispatchReceiver = JsIrBuilder.buildGetValue(thisReceiver)
                    })
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
                    argumentToPropertiesMap.getValue(it).symbol
                }
            }

            function.transform(LiveLocalsTransformer(localToPropertyMap, JsIrBuilder.buildGetValue(thisReceiver), unit), null)
        }

        private fun computeLivenessAtSuspensionPoints(body: IrBody): Map<IrCall, List<IrValueDeclaration>> {
            // TODO: data flow analysis.
            // Just save all visible for now.
            val result = mutableMapOf<IrCall, List<IrValueDeclaration>>()
            body.acceptChildrenVoid(object : VariablesScopeTracker() {
                override fun visitCall(expression: IrCall) {
                    if (!expression.isSuspend) return super.visitCall(expression)

                    expression.acceptChildrenVoid(this)
                    val visibleVariables = mutableListOf<IrValueDeclaration>()
                    scopeStack.forEach { visibleVariables += it }
                    result[expression] = visibleVariables
                }
            })

            return result
        }
    }

    private open class VariablesScopeTracker : IrElementVisitorVoid {

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