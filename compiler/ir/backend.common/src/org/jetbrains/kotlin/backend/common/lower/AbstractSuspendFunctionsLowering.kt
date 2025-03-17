/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
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
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.memoryOptimizedMap
import org.jetbrains.kotlin.utils.memoryOptimizedPlus

abstract class AbstractSuspendFunctionsLowering<C : CommonBackendContext>(val context: C) : FileLoweringPass {
    protected companion object {
        val STATEMENT_ORIGIN_COROUTINE_IMPL = IrStatementOriginImpl("COROUTINE_IMPL")
        val DECLARATION_ORIGIN_COROUTINE_IMPL = IrDeclarationOriginImpl("COROUTINE_IMPL")
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

    protected fun buildCoroutine(function: IrSimpleFunction): IrClass {
        val coroutine = CoroutineBuilder(function).build()
        // Replace original function with a call to constructor of the built coroutine.
        val irBuilder = context.createIrBuilder(function.symbol, UNDEFINED_OFFSET, UNDEFINED_OFFSET)
        val functionBody = function.body as IrBlockBody
        functionBody.statements.clear()
        functionBody.statements.addAll(irBuilder.irBlockBody {
            generateCoroutineStart(coroutine.stateMachineFunction, createCoroutineInstance(function, coroutine))
        }.statements)
        return coroutine.coroutineClass
    }

    protected class BuiltCoroutine(
        val coroutineClass: IrClass,
        val coroutineConstructor: IrConstructor,
        val stateMachineFunction: IrFunction,
    )

    private inner class CoroutineBuilder(val function: IrSimpleFunction) {
        private val functionParameters = function.parameters

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

        private val coroutineConstructors = mutableListOf<IrConstructor>()

        private fun getCoroutineClass(function: IrSimpleFunction): IrClass {
            return buildNewCoroutineClass(function)
        }

        fun build(): BuiltCoroutine {
            coroutineClass.superTypes = mutableListOf(coroutineBaseClass.defaultType)

            val coroutineConstructor = buildConstructor()

            val superInvokeSuspendFunction = coroutineBaseClass.owner.simpleFunctions().single { it.name == stateMachineMethodName }
            val invokeSuspendMethod = buildInvokeSuspendMethod(superInvokeSuspendFunction)

            coroutineClass.addFakeOverrides(context.typeSystem)

            return BuiltCoroutine(coroutineClass, coroutineConstructor, invokeSuspendMethod)
        }

        private fun buildConstructor(): IrConstructor {
            return context.irFactory.buildConstructor {
                origin = DECLARATION_ORIGIN_COROUTINE_IMPL
                visibility = function.visibility
                returnType = coroutineClass.defaultType
                isPrimary = true
            }.apply {
                parent = coroutineClass
                coroutineClass.declarations += this
                coroutineConstructors += this

                parameters = functionParameters.memoryOptimizedMap { parameter ->
                    parameter.copyTo(this, DECLARATION_ORIGIN_COROUTINE_IMPL, kind = IrParameterKind.Regular)
                }
                val continuationParameter = coroutineBaseClassConstructor.parameters[0]
                parameters = parameters memoryOptimizedPlus continuationParameter.copyTo(
                    this, DECLARATION_ORIGIN_COROUTINE_IMPL,
                    startOffset = function.startOffset,
                    endOffset = function.endOffset,
                    type = continuationType,
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
                origin = DECLARATION_ORIGIN_COROUTINE_IMPL
                name = stateMachineFunction.name
                visibility = stateMachineFunction.visibility
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

                overriddenSymbols = overriddenSymbols memoryOptimizedPlus stateMachineFunction.symbol
            }

            buildStateMachine(function, this@CoroutineBuilder.function, argumentToPropertiesMap)
            return function
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
