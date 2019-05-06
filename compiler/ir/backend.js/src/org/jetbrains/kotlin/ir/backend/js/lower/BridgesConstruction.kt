/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.bridges.FunctionHandle
import org.jetbrains.kotlin.backend.common.bridges.generateBridges
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.ir.isMethodOfAny
import org.jetbrains.kotlin.backend.common.ir.isSuspend
import org.jetbrains.kotlin.backend.common.lower.SpecialBridgeMethods
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsLoweredDeclarationOrigin
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.asString
import org.jetbrains.kotlin.ir.backend.js.utils.getJsName
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.isUnit
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
class BridgesConstruction(val context: JsIrBackendContext) : ClassLoweringPass {

    private val specialBridgeMethods = SpecialBridgeMethods(context)

    override fun lower(irClass: IrClass) {
        irClass.declarations
            .asSequence()
            .filterIsInstance<IrSimpleFunction>()
            .filter { !it.isStaticMethodOfClass }
            .toList()
            .forEach { generateBridges(it, irClass) }
    }

    private fun generateBridges(function: IrSimpleFunction, irClass: IrClass) {
        // equals(Any?), hashCode(), toString() never need bridges
        if (function.isMethodOfAny())
            return

        val (specialOverride: IrSimpleFunction?, specialOverrideValueGenerator) =
            specialBridgeMethods.findSpecialWithOverride(function) ?: Pair(null, null)

        val specialOverrideSignature = specialOverride?.let { FunctionAndSignature(it) }

        val bridgesToGenerate = generateBridges(
            function = IrBasedFunctionHandle(function),
            signature = { FunctionAndSignature(it.function) }
        )

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
                    createBridge(function, from.function, to.function, specialOverrideValueGenerator)

                else ->
                    createBridge(function, from.function, to.function, null)
            }


            irClass.declarations.add(bridge)
        }
    }

    private val unitValue = JsIrBuilder.buildGetObjectValue(context.irBuiltIns.unitType, context.irBuiltIns.unitClass)

    // Ported from from jvm.lower.BridgeLowering
    private fun createBridge(
        function: IrSimpleFunction,
        bridge: IrSimpleFunction,
        delegateTo: IrSimpleFunction,
        defaultValueGenerator: ((IrSimpleFunction) -> IrExpression)?
    ): IrFunction {

        val origin =
            if (bridge.isEffectivelyExternal())
                JsLoweredDeclarationOrigin.BRIDGE_TO_EXTERNAL_FUNCTION
            else
                IrDeclarationOrigin.BRIDGE

        // TODO: Support offsets for debug info
        val irFunction = JsIrBuilder.buildFunction(
            bridge.name,
            bridge.returnType,
            function.parent,
            bridge.visibility,
            bridge.modality, // TODO: should copy modality?
            bridge.isInline,
            bridge.isExternal,
            bridge.isTailrec,
            bridge.isSuspend,
            origin
        ).apply {

            // TODO: should dispatch receiver be copied?
            dispatchReceiverParameter = bridge.dispatchReceiverParameter?.run {
                IrValueParameterImpl(startOffset, endOffset, origin, descriptor, type, varargElementType).also { it.parent = this@apply }
            }
            extensionReceiverParameter = bridge.extensionReceiverParameter?.copyTo(this)
            copyTypeParametersFrom(bridge)
            valueParameters += bridge.valueParameters.map { p -> p.copyTo(this) }
            annotations += bridge.annotations
            overriddenSymbols.addAll(delegateTo.overriddenSymbols)
        }

        context.createIrBuilder(irFunction.symbol).irBlockBody(irFunction) {
            if (defaultValueGenerator != null) {
                irFunction.valueParameters.forEach {
                    +irIfThen(
                        context.irBuiltIns.unitType,
                        irNot(irIs(irGet(it), delegateTo.valueParameters[it.index].type)),
                        irReturn(defaultValueGenerator(irFunction))
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

            // This is required for Unit materialization
            // TODO: generalize for boxed types and inline classes
            // TODO: use return type in signature too
            val returnValue = if (delegateTo.returnType.isUnit() && !function.returnType.isUnit()) {
                irComposite(resultType = irFunction.returnType) {
                    +call
                    +unitValue
                }
            } else {
                call
            }
            +irReturn(returnValue)
        }.apply {
            irFunction.body = this
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

    private data class Signature(
        val name: String,
        val extensionReceiverType: String? = null,
        val valueParameters: List<String?> = emptyList(),
        val returnType: String? = null
    )

    private val jsName = function.getJsName()
    private val signature = when {
        jsName != null -> Signature(jsName)
        function.isEffectivelyExternal() -> Signature(function.name.asString())
        else -> Signature(
            function.name.asString(),
            function.extensionReceiverParameter?.type?.asString(),
            function.valueParameters.map { it.type.asString() },
            // Return type used in signature for inline classes only because
            // they are binary incompatible with supertypes and require bridges.
            function.returnType.run { if (isInlined()) asString() else null }
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FunctionAndSignature) return false

        return signature == other.signature
    }

    override fun hashCode(): Int = signature.hashCode()
}


