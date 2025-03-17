/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.coroutines

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.backend.js.lower.CallableReferenceLowering
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.declarations.buildConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.atMostOne
import org.jetbrains.kotlin.utils.memoryOptimizedMap
import org.jetbrains.kotlin.utils.memoryOptimizedPlus

abstract class AbstractSuspendFunctionsLowering<C : JsCommonBackendContext>(val context: C) : BodyLoweringPass {
    companion object {
        val DECLARATION_ORIGIN_COROUTINE_IMPL = IrDeclarationOriginImpl("COROUTINE_IMPL")
        val DECLARATION_ORIGIN_COROUTINE_IMPL_INVOKE by IrDeclarationOriginImpl
    }

    protected abstract val stateMachineMethodName: Name
    protected abstract fun getCoroutineBaseClass(function: IrFunction): IrClassSymbol
    protected abstract fun nameForCoroutineClass(function: IrFunction): Name

    protected abstract fun buildStateMachine(
        stateMachineFunction: IrFunction,
        transformingFunction: IrFunction,
        argumentToPropertiesMap: Map<IrValueParameter, IrField>
    )

    protected abstract fun IrBlockBodyBuilder.generateCoroutineStart(invokeSuspendFunction: IrFunction, receiver: IrExpression)

    protected open fun IrBuilderWithScope.generateDelegatedCall(expectedType: IrType, delegatingCall: IrExpression): IrExpression =
        delegatingCall

    private fun IrBlockBodyBuilder.createCoroutineInstance(function: IrSimpleFunction, parameters: Collection<IrValueParameter>, coroutine: BuiltCoroutine): IrExpression {
        val constructor = coroutine.coroutineConstructor
        val coroutineTypeArgs = function.typeParameters.memoryOptimizedMap {
            IrSimpleTypeImpl(it.symbol, true, emptyList(), emptyList())
        }

        return irCallConstructor(constructor.symbol, coroutineTypeArgs).apply {
            parameters.forEachIndexed { index, argument ->
                putValueArgument(index, irGet(argument))
            }
            putValueArgument(
                parameters.size,
                irCall(
                    getContinuationSymbol,
                    getContinuationSymbol.owner.returnType,
                    listOf(function.returnType)
                )
            )
        }
    }

    protected fun buildCoroutine(function: IrSimpleFunction): IrClass {
        val coroutine = CoroutineBuilder(function).build()

        val isSuspendLambda = coroutine.coroutineClass === function.parent

        if (isSuspendLambda) return coroutine.coroutineClass

        // It is not a lambda - replace original function with a call to constructor of the built coroutine.

        with(function) {
            val irBuilder = context.createIrBuilder(symbol, UNDEFINED_OFFSET, UNDEFINED_OFFSET)
            val functionBody = body as IrBlockBody
            functionBody.statements.clear()
            functionBody.statements.addAll(irBuilder.irBlockBody {
                generateCoroutineStart(coroutine.stateMachineFunction, createCoroutineInstance(this@with, parameters, coroutine))
            }.statements)
        }

        return coroutine.coroutineClass
    }

    private inner class CoroutineBuilder(private val function: IrSimpleFunction) {
        private val isSuspendLambda = function.isOperator && function.name.asString() == "invoke" && function.parentClassOrNull
            ?.let { it.origin === CallableReferenceLowering.LAMBDA_IMPL } == true
        private val functionParameters = if (isSuspendLambda) function.valueParameters else function.parameters

        private val coroutineClass: IrClass = getCoroutineClass(function)

        private fun buildNewCoroutineClass(function: IrSimpleFunction): IrClass =
            context.irFactory.buildClass {
                origin = DECLARATION_ORIGIN_COROUTINE_IMPL
                name = nameForCoroutineClass(function)
                visibility = function.visibility
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
        private val coroutineBaseClassConstructor = coroutineBaseClass.owner.constructors.single { it.valueParameters.size == 1 }
        private val create1Function = coroutineBaseClass.owner.simpleFunctions()
            .single { it.name.asString() == "create" && it.valueParameters.size == 1 }
        private val create1CompletionParameter = create1Function.valueParameters[0]

        private fun getCoroutineClass(function: IrSimpleFunction): IrClass {
            return if (isSuspendLambda) function.parentAsClass
            else buildNewCoroutineClass(function)
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
                name = coroutineBaseClassConstructor.name
                visibility = function.visibility
                returnType = coroutineClass.defaultType
                isPrimary = true
            }.apply {
                parent = coroutineClass
                coroutineClass.addChild(this)

                valueParameters = functionParameters.memoryOptimizedMap { parameter ->
                    parameter.copyTo(this, DECLARATION_ORIGIN_COROUTINE_IMPL, defaultValue = null)
                }
                val continuationParameter = coroutineBaseClassConstructor.valueParameters[0]
                valueParameters = valueParameters memoryOptimizedPlus continuationParameter.copyTo(
                    this, DECLARATION_ORIGIN_COROUTINE_IMPL,
                    startOffset = function.startOffset,
                    endOffset = function.endOffset,
                    type = continuationType,
                    defaultValue = null
                )

                val irBuilder = context.createIrBuilder(symbol, startOffset, endOffset)
                body = irBuilder.irBlockBody {
                    val completionParameter = valueParameters.last()
                    +irDelegatingConstructorCall(coroutineBaseClassConstructor).apply {
                        putValueArgument(0, irGet(completionParameter))
                    }
                    +IrInstanceInitializerCallImpl(startOffset, endOffset, coroutineClass.symbol, context.irBuiltIns.unitType)

                    functionParameters.forEachIndexed { index, parameter ->
                        +irSetField(
                            irGet(coroutineClassThis),
                            argumentToPropertiesMap.getValue(parameter),
                            irGet(valueParameters[index])
                        )
                    }
                }
            }
        }

        private fun buildInvokeSuspendMethod(stateMachineFunction: IrSimpleFunction): IrSimpleFunction {
            val smFunction = context.irFactory.buildFun {
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

            buildStateMachine(smFunction, function, argumentToPropertiesMap)
            return smFunction
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

            // TODO constructing fake overrides on lowered declaration is tricky.
            coroutineClass.declarations.transformFlat {
                if (it is IrProperty && it.isFakeOverride) {
                    listOfNotNull(it.getter, it.setter)
                } else null
            }

            return BuiltCoroutine(
                coroutineClass = coroutineClass,
                coroutineConstructor = coroutineConstructor,
                stateMachineFunction = invokeSuspendMethod
            )

        }
    }

    private val symbols = context.symbols
    private val getContinuationSymbol = symbols.getContinuation
    private val continuationClassSymbol = getContinuationSymbol.owner.returnType.classifierOrFail as IrClassSymbol

    private class BuiltCoroutine(
        val coroutineClass: IrClass,
        val coroutineConstructor: IrConstructor,
        val stateMachineFunction: IrFunction
    )

    protected open class VariablesScopeTracker : IrVisitorVoid() {

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
