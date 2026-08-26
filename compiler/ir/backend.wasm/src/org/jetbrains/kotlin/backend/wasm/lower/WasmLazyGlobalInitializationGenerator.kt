/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.lower.LazyGlobalInitializationGenerator
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irEqeqeq
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irReturnUnit
import org.jetbrains.kotlin.ir.builders.kClassReference
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.types.starProjectedType

class WasmLazyGlobalInitializationGenerator(override val backendContext: WasmBackendContext) : LazyGlobalInitializationGenerator() {
    override fun IrBuilderWithScope.generateStaticInitializationStateChecks(getStateField: IrGetField, klass: IrClass?): List<IrStatement> {
        val errorInitializationBranch = irCall(backendContext.symbols.staticInitializationFailureWithClassName).apply {
            arguments[0] = klass?.let { kClassReference(it.symbol.starProjectedType) } ?: irNull()
        }

        val state = scope.createTemporaryVariable(
            getStateField,
            nameHint = "state",
            inventUniqueName = false,
        )

        return listOf(
            state,
            irIfThen(
                irEqeqeq(irGet(state), irInt(InitializationState.INITIALIZED)),
                irReturnUnit()
            ),
            irIfThen(
                irEqeqeq(irGet(state), irInt(InitializationState.ERROR)),
                errorInitializationBranch
            )
        )
    }
}
