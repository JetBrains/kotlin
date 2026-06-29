/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.backend.js.lower.EnumEntryCreateGetInstancesFunsLowering
import org.jetbrains.kotlin.ir.backend.js.lower.EnumEntryInstancesLowering
import org.jetbrains.kotlin.ir.backend.js.lower.ObjectDeclarationLowering
import org.jetbrains.kotlin.ir.backend.js.lower.WebStaticInitializersDeclarationLowering
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.kClassReference
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.types.starProjectedType

@PhasePrerequisites(
    ObjectDeclarationLowering::class,
    EnumEntryInstancesLowering::class,
    EnumEntryCreateGetInstancesFunsLowering::class,
)
class WasmStaticInitializersDeclarationLowering(override val context: WasmBackendContext) : WebStaticInitializersDeclarationLowering() {
    override fun IrBuilderWithScope.generateStaticInitializationStateCheck(getStateField: IrGetField, container: IrClass): IrCall =
        irCall(this@WasmStaticInitializersDeclarationLowering.context.symbols.checkStaticInitializationState).apply {
            arguments[0] = getStateField
            arguments[1] = kClassReference(container.symbol.starProjectedType)
        }
}
