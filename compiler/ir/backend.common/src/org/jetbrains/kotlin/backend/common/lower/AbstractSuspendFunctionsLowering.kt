/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.collectTailSuspendCalls
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.declarations.buildConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOriginImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
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

    protected abstract fun buildStateMachine(
        stateMachineFunction: IrFunction,
        transformingFunction: IrFunction,
        argumentToPropertiesMap: Map<IrValueParameter, IrField>,
    )

    protected abstract fun IrBlockBodyBuilder.generateCoroutineStart(invokeSuspendFunction: IrFunction, receiver: IrExpression)

    protected abstract fun initializeStateMachine(coroutineConstructors: List<IrConstructor>, coroutineClassThis: IrValueDeclaration)

    protected open fun IrBuilderWithScope.generateDelegatedCall(expectedType: IrType, delegatingCall: IrExpression): IrExpression =
        delegatingCall

    override fun lower(irFile: IrFile) {
        irFile.transformDeclarationsFlat(::tryTransformSuspendFunction)
        irFile.acceptVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitClass(declaration: IrClass) {
                declaration.acceptChildrenVoid(this)
                if (declaration.origin != DECLARATION_ORIGIN_COROUTINE_IMPL)
                    declaration.transformDeclarationsFlat(::tryTransformSuspendFunction)
            }
        })
    }

    private fun tryTransformSuspendFunction(element: IrElement): List<IrDeclaration>? {
        val function = (element as? IrSimpleFunction) ?: return null
        if (!function.isSuspend || function.modality == Modality.ABSTRACT) return null

        val (tailSuspendCalls, hasNotTailSuspendCalls) = collectTailSuspendCalls(context, function)
        return if (hasNotTailSuspendCalls) {
            listOf<IrDeclaration>(buildCoroutine(function).clazz, function)
        } else {
            // Otherwise, no suspend calls at all or all of them are tail calls - no need in a state machine.
            // Have to simplify them though (convert them to proper return statements).
            simplifyTailSuspendCalls(function, tailSuspendCalls)
            null
        }
    }

    protected fun IrCall.isReturnIfSuspendedCall() =
        symbol == context.ir.symbols.returnIfSuspended

    private fun simplifyTailSuspendCalls(irFunction: IrSimpleFunction, tailSuspendCalls: Set<IrCall>) {
        if (tailSuspendCalls.isEmpty()) return

        val irBuilder = context.createIrBuilder(irFunction.symbol)
        irFunction.body!!.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                val shortCut = if (expression.isReturnIfSuspendedCall())
                    expression.arguments[0]!!
                else expression

                shortCut.transformChildrenVoid(this)

                return if (!expression.isSuspend || expression !in tailSuspendCalls)
                    shortCut
                else irBuilder.at(expression).irReturn(
                    irBuilder.generateDelegatedCall(irFunction.returnType, shortCut)
                )
            }
        })
    }

    private val symbols = context.ir.symbols
    private val getContinuationSymbol = symbols.getContinuation
    private val continuationClassSymbol = getContinuationSymbol.owner.returnType.classifierOrFail as IrClassSymbol

    private fun buildCoroutine(irFunction: IrSimpleFunction) =
        CoroutineBuilder(irFunction).build().also { coroutine ->
            // Replace original function with a call to constructor of the built coroutine.
            val irBuilder = context.createIrBuilder(irFunction.symbol, irFunction.startOffset, irFunction.endOffset)
            irFunction.body = irBuilder.irBlockBody(irFunction) {
                val constructor = coroutine.constructor
                generateCoroutineStart(coroutine.stateMachineFunction,
                                       irCallConstructor(constructor.symbol, irFunction.typeParameters.map {
                                           it.defaultType.makeNullable()
                                       }).apply {
                                           val functionParameters = irFunction.parameters
                                           functionParameters.forEachIndexed { index, argument ->
                                               arguments[index] = irGet(argument)
                                           }
                                           arguments[functionParameters.size] =
                                               irCall(
                                                   getContinuationSymbol,
                                                   getContinuationSymbol.owner.returnType,
                                                   listOf(irFunction.returnType)
                                               )
                                       })
            }
        }

    private class BuiltCoroutine(val clazz: IrClass, val constructor: IrConstructor, val stateMachineFunction: IrFunction)

    private inner class CoroutineBuilder(val irFunction: IrFunction) {
        private val functionParameters = irFunction.parameters

        private val coroutineClass: IrClass =
            context.irFactory.buildClass {
                startOffset = irFunction.startOffset
                endOffset = irFunction.endOffset
                origin = DECLARATION_ORIGIN_COROUTINE_IMPL
                name = nameForCoroutineClass(irFunction)
                visibility = DescriptorVisibilities.PRIVATE
            }.apply {
                parent = irFunction.parent
                createThisReceiverParameter()
                typeParameters = irFunction.typeParameters.memoryOptimizedMap { typeParam ->
                    typeParam.copyToWithoutSuperTypes(this).apply { superTypes = superTypes memoryOptimizedPlus typeParam.superTypes }
                }
            }

        private val coroutineClassThis = coroutineClass.thisReceiver!!

        private val continuationType = continuationClassSymbol.typeWith(irFunction.returnType)

        // Save all arguments to fields.
        private val argumentToPropertiesMap = functionParameters.associateWith { coroutineClass.addField(it.name, it.type, false) }

        private val coroutineBaseClass = getCoroutineBaseClass(irFunction)
        private val coroutineBaseClassConstructor = coroutineBaseClass.owner.constructors.single { it.hasShape(regularParameters = 1) }

        private val coroutineConstructors = mutableListOf<IrConstructor>()

        fun build(): BuiltCoroutine {
            coroutineClass.superTypes = mutableListOf(coroutineBaseClass.defaultType)

            val coroutineConstructor = buildConstructor()

            val superInvokeSuspendFunction = coroutineBaseClass.owner.simpleFunctions().single { it.name == stateMachineMethodName }
            val invokeSuspendMethod = buildInvokeSuspendMethod(superInvokeSuspendFunction, coroutineClass)

            coroutineClass.addFakeOverrides(context.typeSystem)

            initializeStateMachine(coroutineConstructors, coroutineClassThis)

            return BuiltCoroutine(coroutineClass, coroutineConstructor, invokeSuspendMethod)
        }

        fun buildConstructor(): IrConstructor =
            context.irFactory.buildConstructor {
                startOffset = irFunction.startOffset
                endOffset = irFunction.endOffset
                origin = DECLARATION_ORIGIN_COROUTINE_IMPL
                visibility = irFunction.visibility
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
                    type = continuationType
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

        private fun buildInvokeSuspendMethod(
            stateMachineFunction: IrSimpleFunction,
            coroutineClass: IrClass
        ): IrSimpleFunction {
            val function = context.irFactory.buildFun {
                startOffset = irFunction.startOffset
                endOffset = irFunction.endOffset
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
                coroutineClass.declarations += this

                typeParameters = stateMachineFunction.typeParameters.memoryOptimizedMap { parameter ->
                    parameter.copyToWithoutSuperTypes(this, origin = DECLARATION_ORIGIN_COROUTINE_IMPL)
                        .apply { superTypes = superTypes memoryOptimizedPlus parameter.superTypes }
                }

                this.parameters += this.createDispatchReceiverParameterWithClassParent()

                this.parameters += stateMachineFunction.nonDispatchParameters
                    .memoryOptimizedMap { parameter ->
                        parameter.copyTo(this, DECLARATION_ORIGIN_COROUTINE_IMPL)
                    }

                overriddenSymbols = overriddenSymbols memoryOptimizedPlus stateMachineFunction.symbol
            }

            buildStateMachine(function, irFunction, argumentToPropertiesMap)
            return function
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
