/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.bridges.FunctionHandle
import org.jetbrains.kotlin.backend.common.bridges.generateBridges
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.varargParameterIndex
import org.jetbrains.kotlin.ir.backend.js.utils.realOverrideTarget
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.utils.memoryOptimizedPlus

/**
 * Constructs bridges for inherited generic functions
 *
 *  Example: for given class hierarchy
 *
 *          class C<T>  {
 *            fun foo(t: T) = ...
 *          }
 *
 *          class D : C<Int> {
 *            override fun foo(t: Int) = impl
 *          }
 *
 *  it adds method D that delegates generic calls to implementation:
 *
 *          class D : C<Int> {
 *            override fun foo(t: Int) = impl
 *            fun foo(t: Any?) = foo(t as Int)  // Constructed bridge
 *          }
 */
abstract class BridgesConstruction<T : JsCommonBackendContext>(val context: T) : DeclarationTransformer {

    private val specialBridgeMethods = SpecialBridgeMethods(context)

    abstract fun getFunctionSignature(function: IrSimpleFunction): Any

    /**
     * Usually just returns [irFunction]'s value parameters, but special transformations may be required if,
     * for example, we're dealing with an external function, and that function contains a vararg,
     * which we must extract and convert to an array.
     */
    protected open fun extractValueParameters(
        blockBodyBuilder: IrBlockBodyBuilder,
        irFunction: IrSimpleFunction,
        bridge: IrSimpleFunction
    ): List<IrValueDeclaration> = irFunction.valueParameters

    // Should dispatch receiver type be casted inside a bridge.
    open val shouldCastDispatchReceiver: Boolean = false

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration !is IrSimpleFunction || declaration.isStaticMethodOfClass || declaration.parent !is IrClass) return null

        return generateBridges(declaration)?.let { listOf(declaration) + it }
    }

    private fun generateBridges(function: IrSimpleFunction): List<IrDeclaration>? {
        val (specialOverride: IrSimpleFunction?, specialOverrideInfo) =
            specialBridgeMethods.findSpecialWithOverride(function) ?: Pair(null, null)

        val specialOverrideSignature = specialOverride?.let { FunctionAndSignature(it) }

        val bridgesToGenerate = generateBridges(
            function = IrBasedFunctionHandle(function),
            signature = { FunctionAndSignature(it.function) }
        )

        if (bridgesToGenerate.isEmpty()) return null

        val result = mutableListOf<IrDeclaration>()

        for ((from, to) in bridgesToGenerate) {
            if (!from.function.parentAsClass.isInterface &&
                from.function.isReal &&
                from.function.modality != Modality.ABSTRACT &&
                !to.function.isReal
            ) {
                continue
            }

            // Don't build bridges for functions with the same signature.
            // TODO: This should be caught earlier in bridgesToGenerate
            if (FunctionAndSignature(to.function.realOverrideTarget) == FunctionAndSignature(from.function.realOverrideTarget))
                continue

            if (from.function.correspondingPropertySymbol != null && from.function.isEffectivelyExternal()) {
                // TODO: Revisit bridges from external properties
                continue
            }

            val bridge: IrDeclaration = when {
                specialOverrideSignature == from ->
                    createBridge(function, from.function, to.function, specialOverrideInfo)

                else ->
                    createBridge(function, from.function, to.function, null)
            }


            result += bridge
        }

        return result
    }

    // Ported from from jvm.lower.BridgeLowering
    private fun createBridge(
        function: IrSimpleFunction,
        bridge: IrSimpleFunction,
        delegateTo: IrSimpleFunction,
        specialMethodInfo: SpecialMethodWithDefaultInfo?
    ): IrFunction {

        val origin = getBridgeOrigin(bridge)

        // TODO: Support offsets for debug info
        val irFunction = context.irFactory.buildFun {
            updateFrom(bridge)
            this.startOffset = UNDEFINED_OFFSET
            this.endOffset = UNDEFINED_OFFSET
            this.name = bridge.name
            this.returnType = bridge.returnType
            this.modality = Modality.FINAL
            this.isFakeOverride = false
            this.origin = origin
        }.apply {
            parent = function.parent
            copyTypeParametersFrom(bridge)
            val substitutionMap = makeTypeParameterSubstitutionMap(bridge, this)
            copyReceiverParametersFrom(bridge, substitutionMap)
            copyValueParametersFrom(bridge, substitutionMap)
            annotations = annotations memoryOptimizedPlus bridge.annotations
            // the js function signature building process (jsFunctionSignature()) uses dfs throught overriddenSymbols for getting js name,
            // therefore it is very important to put bridge symbol at the beginning, it allows to get correct js function name
            overriddenSymbols = overriddenSymbols memoryOptimizedPlus bridge.symbol memoryOptimizedPlus delegateTo.overriddenSymbols
        }

        irFunction.body = context.irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET) {
            statements += context.createIrBuilder(irFunction.symbol).irBlockBody(irFunction) {
                val valueParameters = extractValueParameters(this, irFunction, bridge)
                if (specialMethodInfo != null) {
                    valueParameters.take(specialMethodInfo.argumentsToCheck).forEachIndexed { index, valueDeclaration ->
                        +irIfThen(
                            context.irBuiltIns.unitType,
                            irNot(irIs(irGet(valueDeclaration), delegateTo.valueParameters[index].type)),
                            irReturn(specialMethodInfo.defaultValueGenerator(irFunction))
                        )
                    }
                }

                val call = irCall(delegateTo.symbol)
                val dispatchReceiver = irGet(irFunction.dispatchReceiverParameter!!)

                call.dispatchReceiver = if (shouldCastDispatchReceiver)
                    irCastIfNeeded(dispatchReceiver, delegateTo.dispatchReceiverParameter!!.type)
                else
                    dispatchReceiver

                irFunction.extensionReceiverParameter?.let {
                    call.extensionReceiver = irCastIfNeeded(irGet(it), delegateTo.extensionReceiverParameter!!.type)
                }

                val toTake = valueParameters.size - if (call.isSuspend xor irFunction.isSuspend) 1 else 0

                valueParameters.subList(0, toTake).forEachIndexed { i, valueParameter ->
                    call.putValueArgument(i, irCastIfNeeded(irGet(valueParameter), delegateTo.valueParameters[i].type))
                }

                +irReturn(call)
            }.statements
        }

        return irFunction
    }

    /**
     * Copies the value parameters from [bridge] to [this]. If [bridge] is external and contains a vararg parameter,
     * only copies the parameters before the vararg.
     * The rest parameters are expected to be obtained later using the `arguments` object in JS.
     */
    private fun IrSimpleFunction.copyValueParametersFrom(bridge: IrSimpleFunction, substitutionMap: Map<IrTypeParameterSymbol, IrType>) {
        var valueParametersToCopy = bridge.valueParameters
        if (bridge.isEffectivelyExternal()) {
            val varargIndex = bridge.varargParameterIndex()
            if (varargIndex != -1) {
                valueParametersToCopy = bridge.valueParameters.take(varargIndex)
            }
        }
        valueParameters = valueParameters memoryOptimizedPlus valueParametersToCopy.map { p -> p.copyTo(this, type = p.type.substitute(substitutionMap)) }
    }

    abstract fun getBridgeOrigin(bridge: IrSimpleFunction): IrDeclarationOrigin

    // TODO: get rid of Unit check
    private fun IrBlockBodyBuilder.irCastIfNeeded(argument: IrExpression, type: IrType): IrExpression =
        if (argument.type.classifierOrNull == type.classifierOrNull) argument else irAs(argument, type)

    // Wrapper around function that compares and hashCodes it based on signature
    // Designed to be used as a Signature type parameter in backend.common.bridges
    inner class FunctionAndSignature(val function: IrSimpleFunction) {

        // TODO: Use type-upper-bound-based signature instead of Strings
        // Currently strings are used for compatibility with a hack-based name generator

        private val signature = getFunctionSignature(function)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is BridgesConstruction<*>.FunctionAndSignature) return false

            return signature == other.signature
        }

        override fun hashCode(): Int = signature.hashCode()
    }
}

// Handle for common.bridges
data class IrBasedFunctionHandle(val function: IrSimpleFunction) : FunctionHandle {
    override val isDeclaration = function.run { isReal || findInterfaceImplementation() != null }

    override val isAbstract: Boolean =
        function.modality == Modality.ABSTRACT

    override val mayBeUsedAsSuperImplementation =
        !function.parentAsClass.isInterface

    override fun getOverridden() =
        function.overriddenSymbols.map { IrBasedFunctionHandle(it.owner) }
}
