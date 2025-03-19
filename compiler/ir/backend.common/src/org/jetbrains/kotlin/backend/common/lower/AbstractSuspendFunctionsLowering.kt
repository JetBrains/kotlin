/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.capturedFields
import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.declarations.buildConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOriginImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.atMostOne
import org.jetbrains.kotlin.utils.memoryOptimizedMap
import org.jetbrains.kotlin.utils.memoryOptimizedPlus

abstract class AbstractSuspendFunctionsLowering<C : CommonBackendContext>(val context: C) : FileLoweringPass {
    companion object {
        val STATEMENT_ORIGIN_COROUTINE_IMPL = IrStatementOriginImpl("COROUTINE_IMPL")
        val DECLARATION_ORIGIN_COROUTINE_IMPL = IrDeclarationOriginImpl("COROUTINE_IMPL")
        val DECLARATION_ORIGIN_COROUTINE_IMPL_INVOKE = IrDeclarationOriginImpl("COROUTINE_IMPL_INVOKE")
    }

    protected abstract val stateMachineMethodName: Name
    protected abstract fun getCoroutineBaseClass(function: IrFunction): IrClassSymbol
    protected abstract fun nameForCoroutineClass(function: IrFunction): Name
    protected open fun coroutineClassVisibility(function: IrFunction): DescriptorVisibility = function.visibility

    protected abstract fun buildStateMachine(
        stateMachineFunction: IrFunction,
        transformingFunction: IrFunction,
        argumentToPropertiesMap: Map<IrValueParameter, IrField>,
    )

    protected abstract fun IrBlockBodyBuilder.generateCoroutineStart(invokeSuspendFunction: IrFunction, receiver: IrExpression)

    protected open fun IrBuilderWithScope.generateDelegatedCall(expectedType: IrType, delegatingCall: IrExpression): IrExpression =
        delegatingCall

    private val symbols = context.symbols
    private val getContinuationSymbol = symbols.getContinuation
    private val continuationClassSymbol = getContinuationSymbol.owner.returnType.classifierOrFail as IrClassSymbol

    private fun IrBlockBodyBuilder.createCoroutineInstance(function: IrSimpleFunction, coroutine: BuiltCoroutine): IrConstructorCall {
        val constructor = coroutine.coroutineConstructor
        val coroutineTypeArgs = function.typeParameters.memoryOptimizedMap {
            it.defaultType.makeNullable()
        }
        return irCallConstructor(constructor.symbol, coroutineTypeArgs).apply {
            val functionParameters = function.parameters
            functionParameters.forEachIndexed { index, argument ->
                arguments[index] = irGet(argument)
            }
            arguments[functionParameters.size] =
                irCall(
                    getContinuationSymbol,
                    getContinuationSymbol.owner.returnType,
                    listOf(function.returnType)
                )
        }
    }

    protected fun buildCoroutine(function: IrSimpleFunction, isSuspendLambdaInvokeMethod: Boolean): IrClass {
        val coroutine = CoroutineBuilder(function, isSuspendLambdaInvokeMethod).build()
        if (!isSuspendLambdaInvokeMethod) {
            // Replace the original function with a call to the constructor of the built coroutine class.
            val irBuilder = context.createIrBuilder(function.symbol, UNDEFINED_OFFSET, UNDEFINED_OFFSET)
            val functionBody = function.body as IrBlockBody
            functionBody.statements.clear()
            functionBody.statements.addAll(irBuilder.irBlockBody {
                generateCoroutineStart(coroutine.stateMachineFunction, createCoroutineInstance(function, coroutine))
            }.statements)
        }
        return coroutine.coroutineClass
    }

    protected class BuiltCoroutine(
        val coroutineClass: IrClass,
        val coroutineConstructor: IrConstructor,
        val stateMachineFunction: IrFunction,
    )

    private inner class CoroutineBuilder(val function: IrSimpleFunction, private val isSuspendLambda: Boolean) {
        private val functionParameters = if (isSuspendLambda) function.nonDispatchParameters else function.parameters

        private val coroutineClass: IrClass = getCoroutineClass(function)

        private fun buildNewCoroutineClass(function: IrSimpleFunction): IrClass =
            context.irFactory.buildClass {
                origin = DECLARATION_ORIGIN_COROUTINE_IMPL
                name = nameForCoroutineClass(function)
                visibility = coroutineClassVisibility(function)
            }.apply {
                parent = function.parent
                createThisReceiverParameter()
                typeParameters = function.typeParameters.memoryOptimizedMap { typeParam ->
                    // TODO: remap types
                    typeParam.copyToWithoutSuperTypes(this).apply { superTypes = superTypes memoryOptimizedPlus typeParam.superTypes }
                }
            }

        private val coroutineClassThis = coroutineClass.thisReceiver!!

        private val continuationType = continuationClassSymbol.typeWith(function.returnType)

        // Save all arguments to fields.
        private val argumentToPropertiesMap = functionParameters
            .associateWith { coroutineClass.addField(it.name, it.type, false) }

        private val coroutineBaseClass = getCoroutineBaseClass(function)
        private val coroutineBaseClassConstructor = coroutineBaseClass.owner.constructors.single { it.hasShape(regularParameters = 1) }

        private fun getCoroutineClass(function: IrSimpleFunction): IrClass {
            return if (isSuspendLambda) function.parentAsClass
            else buildNewCoroutineClass(function)
        }

        fun build(): BuiltCoroutine {
            val coroutineConstructor = buildConstructor()

            val implementedMembers = ArrayList<IrSimpleFunction>(2)

            val superInvokeSuspendFunction = coroutineBaseClass.owner.simpleFunctions().single { it.name == stateMachineMethodName }
            val invokeSuspendMethod = buildInvokeSuspendMethod(superInvokeSuspendFunction)

            implementedMembers.add(invokeSuspendMethod)

            if (isSuspendLambda) {
                // Suspend lambda - create factory methods.
                val createFunction = coroutineBaseClass.owner.simpleFunctions()
                    .atMostOne {
                        it.name.asString() == "create" && it.valueParameters.size == function.valueParameters.size + 1
                    }

                val createMethod = buildCreateMethod(createFunction, coroutineConstructor)
                implementedMembers.add(createMethod)

                transformInvokeMethod(createMethod, invokeSuspendMethod)
            } else {
                coroutineClass.superTypes = coroutineClass.superTypes memoryOptimizedPlus coroutineBaseClass.defaultType
            }

            coroutineClass.addFakeOverrides(context.typeSystem, implementedMembers)

            return BuiltCoroutine(coroutineClass, coroutineConstructor, invokeSuspendMethod)
        }

        private fun buildConstructor(): IrConstructor {
            if (isSuspendLambda) {
                return coroutineClass.declarations
                    .filterIsInstance<IrConstructor>()
                    .single()
                    .let {
                        context.mapping.capturedConstructors[it] ?: it
                    }
            }

            return context.irFactory.buildConstructor {
                origin = DECLARATION_ORIGIN_COROUTINE_IMPL
                visibility = function.visibility
                returnType = coroutineClass.defaultType
                isPrimary = true
            }.apply {
                parent = coroutineClass
                coroutineClass.declarations += this

                parameters = functionParameters.memoryOptimizedMap { parameter ->
                    parameter.copyTo(this, DECLARATION_ORIGIN_COROUTINE_IMPL, defaultValue = null, kind = IrParameterKind.Regular)
                }
                val continuationParameter = coroutineBaseClassConstructor.parameters[0]
                parameters = parameters memoryOptimizedPlus continuationParameter.copyTo(
                    this, DECLARATION_ORIGIN_COROUTINE_IMPL,
                    startOffset = function.startOffset,
                    endOffset = function.endOffset,
                    type = continuationType,
                    defaultValue = null,
                )

                val irBuilder = context.createIrBuilder(symbol, startOffset, endOffset)
                body = irBuilder.irBlockBody {
                    val completionParameter = parameters.last()
                    +irDelegatingConstructorCall(coroutineBaseClassConstructor).apply {
                        arguments[0] = irGet(completionParameter)
                    }
                    +IrInstanceInitializerCallImpl(startOffset, endOffset, coroutineClass.symbol, context.irBuiltIns.unitType)

                    functionParameters.forEachIndexed { index, parameter ->
                        +irSetField(
                            irGet(coroutineClassThis),
                            argumentToPropertiesMap.getValue(parameter),
                            irGet(parameters[index])
                        )
                    }
                }
            }
        }

        private fun buildInvokeSuspendMethod(stateMachineFunction: IrSimpleFunction): IrSimpleFunction {
            val function = context.irFactory.buildFun {
                startOffset = function.startOffset
                endOffset = function.endOffset
                origin = DECLARATION_ORIGIN_COROUTINE_IMPL_INVOKE
                name = stateMachineFunction.name
                visibility = stateMachineFunction.visibility
                modality = Modality.FINAL
                returnType = context.irBuiltIns.anyNType
                isInline = stateMachineFunction.isInline
                isExternal = stateMachineFunction.isExternal
                isTailrec = stateMachineFunction.isTailrec
                isSuspend = stateMachineFunction.isSuspend
                isOperator = false
                isExpect = stateMachineFunction.isExpect
                isFakeOverride = false
            }.apply {
                parent = coroutineClass
                addOverriddenChildToCoroutineClass(this, stateMachineFunction)

                typeParameters = stateMachineFunction.typeParameters.memoryOptimizedMap { parameter ->
                    parameter.copyToWithoutSuperTypes(this, origin = DECLARATION_ORIGIN_COROUTINE_IMPL)
                        .apply { superTypes = superTypes memoryOptimizedPlus parameter.superTypes }
                }

                parameters += createDispatchReceiverParameterWithClassParent()

                parameters += stateMachineFunction.nonDispatchParameters
                    .memoryOptimizedMap { parameter ->
                        parameter.copyTo(this, DECLARATION_ORIGIN_COROUTINE_IMPL)
                    }

                overriddenSymbols = listOf(stateMachineFunction.symbol)
            }

            buildStateMachine(function, this@CoroutineBuilder.function, argumentToPropertiesMap)
            return function
        }

        // val i = $lambdaN(this.f1, this.f2, ..., this.fn, continuation) // bound
        // i.s1 = p1 // unbound
        // ...
        // i.sn = pn
        // return i
        private fun buildCreateMethod(superCreateFunction: IrSimpleFunction?, constructor: IrConstructor): IrSimpleFunction =
            context.irFactory.buildFun {
                startOffset = UNDEFINED_OFFSET
                endOffset = UNDEFINED_OFFSET
                origin = DECLARATION_ORIGIN_COROUTINE_IMPL
                name = Name.identifier("create")
                visibility = DescriptorVisibilities.PROTECTED
                returnType = coroutineClass.defaultType
            }.apply {
                parent = coroutineClass
                addOverriddenChildToCoroutineClass(this, superCreateFunction)

                typeParameters = function.typeParameters.memoryOptimizedMap { parameter ->
                    parameter.copyToWithoutSuperTypes(this, origin = DECLARATION_ORIGIN_COROUTINE_IMPL)
                        .apply { superTypes = superTypes memoryOptimizedPlus parameter.superTypes }
                }

                val unboundArgs = function.valueParameters

                val create1Function = coroutineBaseClass.owner.simpleFunctions()
                    .single { it.name.asString() == "create" && it.valueParameters.size == 1 }
                val create1CompletionParameter = create1Function.valueParameters[0]

                val createValueParameters = (unboundArgs + create1CompletionParameter).memoryOptimizedMap { parameter ->
                    parameter.copyTo(this, DECLARATION_ORIGIN_COROUTINE_IMPL)
                }

                this.parameters += this.createDispatchReceiverParameterWithClassParent()

                this.parameters += createValueParameters

                superCreateFunction?.let {
                    overriddenSymbols = ArrayList<IrSimpleFunctionSymbol>(it.overriddenSymbols.size + 1).apply {
                        addAll(it.overriddenSymbols)
                        add(it.symbol)
                    }
                }

                val thisReceiver = this.dispatchReceiverParameter!!

                val boundFields = coroutineClass.capturedFields
                    ?: compilationException(
                        "No captured values",
                        coroutineClass
                    )

                val irBuilder = context.createIrBuilder(symbol, startOffset, endOffset)
                body = irBuilder.irBlockBody(startOffset, endOffset) {
                    val instanceCreate = irCall(constructor).apply {
                        var unboundIndex = 0
                        for (f in boundFields) {
                            putValueArgument(unboundIndex++, irGetField(irGet(thisReceiver), f))
                        }
                        putValueArgument(unboundIndex++, irGet(createValueParameters.last()))
                        assert(unboundIndex == constructor.valueParameters.size) {
                            "Not all arguments of <create> are used"
                        }
                    }
                    val instanceVal = scope.createTmpVariable(instanceCreate, "i")
                    +instanceVal

                    assert(createValueParameters.size - 1 == argumentToPropertiesMap.size)

                    for ((p, f) in createValueParameters.zip(argumentToPropertiesMap.values)) {
                        +irSetField(irGet(instanceVal), f, irGet(p))
                    }

                    +irReturn(irGet(instanceVal))
                }
            }

        private fun addOverriddenChildToCoroutineClass(newFunction: IrSimpleFunction, superFunction: IrSimpleFunction?) {
            val fakeOverrideIndex = superFunction?.let { superFunction ->
                coroutineClass
                    .declarations
                    .indexOfFirst {
                        it is IrOverridableDeclaration<*> && superFunction.symbol in it.overriddenSymbols && it.isFakeOverride
                    }
            } ?: -1

            if (fakeOverrideIndex >= 0) {
                coroutineClass.declarations[fakeOverrideIndex] = newFunction
            } else {
                coroutineClass.declarations.add(newFunction)
            }
        }

        private fun transformInvokeMethod(createFunction: IrSimpleFunction, stateMachineFunction: IrSimpleFunction) {
            val irBuilder = context.createIrBuilder(function.symbol, UNDEFINED_OFFSET, UNDEFINED_OFFSET)
            val thisReceiver = function.dispatchReceiverParameter
                ?: compilationException(
                    "Expected dispatch receiver for invoke",
                    function
                )
            val functionBody = function.body as IrBlockBody
            functionBody.statements.clear()
            functionBody.statements.addAll(irBuilder.irBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET) {
                generateCoroutineStart(stateMachineFunction, irCall(createFunction).apply {
                    dispatchReceiver = irGet(thisReceiver)
                    var index = 0
                    for (parameter in function.valueParameters) {
                        putValueArgument(index++, irGet(parameter))
                    }
                    putValueArgument(
                        index++,
                        irCall(getContinuationSymbol, getContinuationSymbol.owner.returnType, listOf(function.returnType))
                    )
                    assert(index == createFunction.valueParameters.size)
                })
            }.statements)
        }
    }

    fun IrClass.addField(name: Name, type: IrType, isMutable: Boolean): IrField {
        val klass = this
        return factory.buildField {
            this.startOffset = klass.startOffset
            this.endOffset = klass.endOffset
            this.origin = DECLARATION_ORIGIN_COROUTINE_IMPL
            this.name = name
            this.type = type
            this.visibility = DescriptorVisibilities.PRIVATE
            this.isFinal = !isMutable
        }.also {
            it.parent = this
            addChild(it)
        }
    }
}
