/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.bridges.FunctionHandle
import org.jetbrains.kotlin.backend.common.bridges.generateBridges
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.ir.isMethodOfAny
import org.jetbrains.kotlin.backend.common.ir.isSuspend
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.DescriptorBasedIr
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsLoweredDeclarationOrigin
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.functionSignature
import org.jetbrains.kotlin.ir.backend.js.utils.getJsName
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.util.*

// Constructs bridges for inherited generic functions
//
//  Example: for given class hierarchy
//
//          class C<T>  {
//            fun foo(t: T) = ...
//          }
//
//          class D : C<Int> {
//            override fun foo(t: Int) = impl
//          }
//
//  it adds method D that delegates generic calls to implementation:
//
//          class D : C<Int> {
//            override fun foo(t: Int) = impl
//            fun foo(t: Any?) = foo(t as Int)  // Constructed bridge
//          }
//
@OptIn(DescriptorBasedIr::class)
class BridgesConstruction(val context: CommonBackendContext) : DeclarationTransformer {

    private val specialBridgeMethods = SpecialBridgeMethods(context)

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration !is IrSimpleFunction || declaration.isStaticMethodOfClass || declaration.parent !is IrClass) return null

        return generateBridges(declaration)?.let { listOf(declaration) + it }
    }

    private fun generateBridges(function: IrSimpleFunction): List<IrDeclaration>? {
        // equals(Any?), hashCode(), toString() never need bridges
        if (function.isMethodOfAny())
            return null

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

        val origin =
            if (bridge.isEffectivelyExternal() || bridge.getJsName() != null)
                JsLoweredDeclarationOrigin.BRIDGE_TO_EXTERNAL_FUNCTION
            else
                IrDeclarationOrigin.BRIDGE

        // TODO: Support offsets for debug info
        val irFunction = JsIrBuilder.buildFunction(
            bridge.name,
            bridge.returnType,
            function.parent,
            bridge.visibility,
            Modality.FINAL,
            isInline = bridge.isInline,
            isExternal = bridge.isExternal,
            isTailrec = bridge.isTailrec,
            isSuspend = bridge.isSuspend,
            isExpect = bridge.isExpect,
            isFakeOverride = false,
            origin = origin
        ).apply {
            copyTypeParametersFrom(bridge)
            // TODO: should dispatch receiver be copied?
            dispatchReceiverParameter = bridge.dispatchReceiverParameter?.run {
                IrValueParameterImpl(startOffset, endOffset, origin, descriptor, type, varargElementType).also { it.parent = this@apply }
            }
            extensionReceiverParameter = bridge.extensionReceiverParameter?.copyTo(this)
            valueParameters += bridge.valueParameters.map { p -> p.copyTo(this) }
            annotations += bridge.annotations
            overriddenSymbols += delegateTo.overriddenSymbols
            overriddenSymbols += bridge.symbol
        }

        irFunction.body = IrBlockBodyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET) {
            statements += context.createIrBuilder(irFunction.symbol).irBlockBody(irFunction) {
                if (specialMethodInfo != null) {
                    irFunction.valueParameters.take(specialMethodInfo.argumentsToCheck).forEach {
                        +irIfThen(
                            context.irBuiltIns.unitType,
                            irNot(irIs(irGet(it), delegateTo.valueParameters[it.index].type)),
                            irReturn(specialMethodInfo.defaultValueGenerator(irFunction))
                        )
                    }
                }

                val call = irCall(delegateTo.symbol)
                call.dispatchReceiver = irGet(irFunction.dispatchReceiverParameter!!)
                irFunction.extensionReceiverParameter?.let {
                    call.extensionReceiver = irCastIfNeeded(irGet(it), delegateTo.extensionReceiverParameter!!.type)
                }

                val toTake = irFunction.valueParameters.size - if (call.isSuspend xor irFunction.isSuspend) 1 else 0

                irFunction.valueParameters.subList(0, toTake).mapIndexed { i, valueParameter ->
                    call.putValueArgument(i, irCastIfNeeded(irGet(valueParameter), delegateTo.valueParameters[i].type))
                }

                +irReturn(call)
            }.statements
        }

        return irFunction
    }

    // TODO: get rid of Unit check
    private fun IrBlockBodyBuilder.irCastIfNeeded(argument: IrExpression, type: IrType): IrExpression =
        if (argument.type.classifierOrNull == type.classifierOrNull) argument else irAs(argument, type)
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

// Wrapper around function that compares and hashCodes it based on signature
// Designed to be used as a Signature type parameter in backend.common.bridges
class FunctionAndSignature(val function: IrSimpleFunction) {

    // TODO: Use type-upper-bound-based signature instead of Strings
    // Currently strings are used for compatibility with a hack-based name generator

    private val signature = functionSignature(function)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FunctionAndSignature) return false

        return signature == other.signature
    }

    override fun hashCode(): Int = signature.hashCode()
}


