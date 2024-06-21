/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ic

import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmCompiledFileFragment
import org.jetbrains.kotlin.backend.wasm.serialization.WasmSerializer
import org.jetbrains.kotlin.ir.backend.js.ic.IrModule
import org.jetbrains.kotlin.ir.backend.js.ic.IrProgramFragments
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsIrProgramFragment
import java.io.OutputStream

class WasmIrProgramFragments(
    override val mainFragment: WasmCompiledFileFragment,
) : IrProgramFragments() {

    override val exportFragment: WasmCompiledFileFragment? = null

    override fun serialize(stream: OutputStream) {
        WasmSerializer(stream).serialize(this.mainFragment)
    }
}

class WasmIrModule(
    override val moduleName: String,
    override val fragments: List<WasmCompiledFileFragment>,
) : IrModule()