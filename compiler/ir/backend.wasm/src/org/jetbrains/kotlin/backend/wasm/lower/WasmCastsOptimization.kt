/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.lower.optimizations.AbstractCastsOptimization
import org.jetbrains.kotlin.backend.common.lower.optimizations.AbstractComputeTypesPass
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.ir2wasm.isBuiltInWasmRefType
import org.jetbrains.kotlin.backend.wasm.castIsProvenToSucceed
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irImplicitCast
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.erasedUpperBound
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.isNullable

/**
 * See [AbstractComputeTypesPass].
 */
class WasmComputeTypesPass(private val wasmContext: WasmBackendContext) : AbstractComputeTypesPass(wasmContext) {
    override fun IrType.getInlinedClassOrNull(): IrClass? = wasmContext.inlineClassesUtils.getInlinedClass(this)

    // The `kotlin.wasm.internal.reftypes` types (`typedfuncref`, `typedcontref`, ...) are lowered by reading their
    // type arguments, so they must keep the exact type the earlier lowerings gave them.
    override fun isTypeReplacementAllowed(type: IrType): Boolean = !isBuiltInWasmRefType(type)

    override fun IrBuilderWithScope.irProvenImplicitCast(argument: IrExpression, type: IrType): IrExpression =
        irWasmProvenImplicitCast(argument, type)
}

// Wasm checks every implicit cast at runtime, so the casts these passes introduce themselves have to be marked as
// proven - otherwise the optimizations would just trade the checks they remove for new ones. The marker is an
// attribute on the cast rather than a wrapping block, so that it survives the lowerings which run in between
// (autoboxing in particular rewrites the expressions inside such a block).
private fun IrBuilderWithScope.irWasmProvenImplicitCast(argument: IrExpression, type: IrType): IrExpression =
    (irImplicitCast(argument, type) as IrTypeOperatorCall).apply { castIsProvenToSucceed = true }

/**
 * See [AbstractCastsOptimization].
 *
 * Wasm generates a runtime check for every cast, including the implicit ones the inliner leaves behind on generic
 * type boundaries (KT-87090). This pass removes the checks which are statically known to succeed or fail, which
 * would otherwise cost both code size and performance.
 */
class WasmCastsOptimization(private val wasmContext: WasmBackendContext) : AbstractCastsOptimization(wasmContext) {
    override fun IrType.getInlinedClassOrNull(): IrClass? = wasmContext.inlineClassesUtils.getInlinedClass(this)

    override fun IrBuilderWithScope.irProvenImplicitCast(argument: IrExpression, type: IrType): IrExpression =
        irWasmProvenImplicitCast(argument, type)

    // Mirrors `WasmBaseTypeOperatorTransformer.generateCCE`. Note that the `Any?` cast below is an implicit one,
    // so `WasmTypeOperatorLowering` lowers it into the plain narrowing (including boxing) without a type check.
    override fun IrBuilderWithScope.irThrowClassCastException(argument: IrExpression, typeOperand: IrType): IrExpression {
        val klass = typeOperand.erasedUpperBound

        if (klass.isEffectivelyExternal() && klass.isInterface) {
            return irCall(wasmContext.wasmSymbols.throwTypeCastException)
        }

        return irCall(wasmContext.wasmSymbols.throwTypeCastWithInfoException).apply {
            arguments[0] = irImplicitCast(argument, wasmContext.irBuiltIns.anyNType)
            arguments[1] = irCall(wasmContext.reflectionSymbols.getKClass).apply {
                typeArguments[0] = klass.defaultType
            }
            arguments[2] = irBoolean(typeOperand.isNullable())
        }
    }
}
