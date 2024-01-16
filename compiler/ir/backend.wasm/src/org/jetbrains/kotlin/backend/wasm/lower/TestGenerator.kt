/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.backend.js.lower.generateJsTests
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrBlockBody

fun generateWasmTests(context: WasmBackendContext, moduleFragment: IrModuleFragment) {
    generateJsTests(
        context = context,
        moduleFragment = moduleFragment,
        groupByPackage = true
    )

    if (context.testEntryPoints.isEmpty())
        return
    require(context.wasmSymbols.startUnitTests != null) { "kotlin.test package must be present" }

    val builder = context.createIrBuilder(context.wasmSymbols.startUnitTests)
    val startFunctionBody = context.wasmSymbols.startUnitTests.owner.body as IrBlockBody

    context.testEntryPoints.forEach { testEntry ->
        startFunctionBody.statements.add(
            builder.irCall(testEntry)
        )
    }
}
