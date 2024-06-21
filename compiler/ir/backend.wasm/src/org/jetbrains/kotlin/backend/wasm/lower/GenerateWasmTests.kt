/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.ModuleLoweringPass
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.backend.js.lower.generateJsTests
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

internal class GenerateWasmTests(private val context: WasmBackendContext) : ModuleLoweringPass {
    override fun lower(irModule: IrModuleFragment) {
        generateJsTests(
            context = context,
            moduleFragment = irModule,
            groupByPackage = true
        )
    }
}
