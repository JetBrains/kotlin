/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.capturedConstructor
import org.jetbrains.kotlin.backend.common.capturedFields
import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.declarations.buildConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOriginImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.atMostOne
import org.jetbrains.kotlin.utils.memoryOptimizedMap
import org.jetbrains.kotlin.utils.memoryOptimizedPlus

abstract class AbstractSuspendFunctionsLowering<C : CommonBackendContext>(val context: C) {
    companion object {
        val STATEMENT_ORIGIN_COROUTINE_IMPL = IrStatementOriginImpl("COROUTINE_IMPL")
        val DECLARATION_ORIGIN_COROUTINE_IMPL = IrDeclarationOriginImpl("COROUTINE_IMPL")
        val DECLARATION_ORIGIN_COROUTINE_IMPL_INVOKE = IrDeclarationOriginImpl("COROUTINE_IMPL_INVOKE")

        private val CREATE_METHOD_NAME = Name.identifier("create")
    }

    protected open val coroutineClassAnnotations: List<IrConstructorCall>
        get() = emptyList()

    protected abstract val stateMachineMethodName: Name
    protected abstract fun getCoroutineBaseClass(function: IrFunction): IrClassSymbol
    protected abstract fun nameForCoroutineClass(function: IrFunction): Name

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

    private fun replaceBodyWithCoroutineClassInstantiation(
        originalFunction: IrSimpleFunction,
        stateMachineFunction: IrFunction,
        constructor: IrFunction,
    ) {
        val irBuilder = context.createIrBuilder(originalFunction.symbol, UNDEFINED_OFFSET, UNDEFINED_OFFSET)
        val functionBody = originalFunction.body as IrBlockBody
        functionBody.statements.clear()
        functionBody.statements.addAll(irBuilder.irBlockBody {
            generateCoroutineStart(
                stateMachineFunction,
                irCall(constructor).apply {
                    originalFunction.typeParameters.mapTo(typeArguments) { it.defaultType.makeNullable() }
                    val functionParameters = originalFunction.parameters
                    functionParameters.forEachIndexed { index, argument ->
                        arguments[index] = this@irBlockBody.irGet(argument)
                    }
                    arguments[functionParameters.size] =
                        this@irBlockBody.irCall(
                            getContinuationSymbol,
                            getContinuationSymbol.owner.returnType,
                            listOf(originalFunction.returnType)
                        )
                },
            )
        }.statements)
    }

    protected fun buildCoroutine(function: IrSimpleFunction, isSuspendLambdaInvokeMethod: Boolean): IrClass {
        return CoroutineBuilder(function, isSuspendLambdaInvokeMethod).build()
    }

    private inner class CoroutineBuilder(val function: IrSimpleFunction, private val isSuspendLambda: Boolean) {
        private val functionParameters = if (isSuspendLambda) function.nonDispatchParameters else function.parameters

        private val coroutineClass: IrClass = getCoroutineClass(function).apply {
            annotations = annotations memoryOptimizedPlus coroutineClassAnnotations
        }

        private fun buildNewCoroutineClass(function: IrSimpleFunction): IrClass =
            context.irFactory.buildClass {
                origin = DECLARATION_ORIGIN_COROUTINE_IMPL
                name = nameForCoroutineClass(function)
                visibility = DescriptorVisibilities.PRIVATE
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

        fun build(): IrClass {
            val coroutineConstructor = buildConstructor()

            val superInvokeSuspendFunction = coroutineBaseClass.owner.simpleFunctions().single { it.name == stateMachineMethodName }
            val invokeSuspendMethod = buildInvokeSuspendMethod(superInvokeSuspendFunction)


            if (isSuspendLambda) {
                // Suspend lambda - create factory methods.
                val createFunction = coroutineBaseClass.owner.simpleFunctions()
                    .atMostOne { it.name == CREATE_METHOD_NAME && it.parameters.size == function.parameters.size + 1 }

                val createMethod = buildCreateMethod(createFunction, coroutineConstructor)

                replaceBodyWithCoroutineClassInstantiation(function, invokeSuspendMethod, createMethod)
            } else {
                coroutineClass.superTypes = coroutineClass.superTypes memoryOptimizedPlus coroutineBaseClass.defaultType
                replaceBodyWithCoroutineClassInstantiation(function, invokeSuspendMethod, coroutineConstructor)
            }

            coroutineClass.addFakeOverrides(context.typeSystem)
            return coroutineClass
        }

        private fun buildConstructor(): IrConstructor {
            if (isSuspendLambda) {
                return coroutineClass.declarations
                    .filterIsInstance<IrConstructor>()
                    .single()
                    .let {
                        it.capturedConstructor ?: it
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

                parameters = listOf(createDispatchReceiverParameterWithClassParent()) +
                        stateMachineFunction.nonDispatchParameters.map { parameter ->
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
                name = CREATE_METHOD_NAME
                visibility = DescriptorVisibilities.PROTECTED
                returnType = coroutineClass.defaultType
            }.apply {
                parent = coroutineClass
                addOverriddenChildToCoroutineClass(this, superCreateFunction)

                typeParameters = function.typeParameters.memoryOptimizedMap { parameter ->
                    parameter.copyToWithoutSuperTypes(this, origin = DECLARATION_ORIGIN_COROUTINE_IMPL)
                        .apply { superTypes = superTypes memoryOptimizedPlus parameter.superTypes }
                }

                val unboundArgs = functionParameters

                val create1Function = coroutineBaseClass.owner.simpleFunctions()
                    .single { it.name == CREATE_METHOD_NAME && it.parameters.size == 2 }
                val create1CompletionParameter = create1Function.parameters[1]

                val createValueParameters = (unboundArgs + create1CompletionParameter).memoryOptimizedMap { parameter ->
                    parameter.copyTo(this, DECLARATION_ORIGIN_COROUTINE_IMPL)
                }
                parameters = listOf(createDispatchReceiverParameterWithClassParent()) + createValueParameters

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
                            arguments[unboundIndex++] = irGetField(irGet(thisReceiver), f)
                        }
                        arguments[unboundIndex++] = irGet(createValueParameters.last())
                        assert(unboundIndex == constructor.parameters.size) {
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
