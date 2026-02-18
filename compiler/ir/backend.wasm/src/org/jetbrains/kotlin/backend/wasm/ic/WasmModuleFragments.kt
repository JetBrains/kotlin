/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ic

import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmCompiledCodeFileFragment
import org.jetbrains.kotlin.backend.wasm.serialization.WasmSerializer
import org.jetbrains.kotlin.ir.backend.js.ic.IrICModule
import org.jetbrains.kotlin.ir.backend.js.ic.IrICProgramFragments
import java.io.OutputStream

open class WasmIrProgramFragments(
    val mainFragment: WasmCompiledCodeFileFragment,
) : IrICProgramFragments() {
    override fun serialize(stream: OutputStream) {
        with(WasmSerializer(stream)) {
            serializeCompiledTypes(mainFragment.definedTypes)
            serializeCompiledDeclarations(mainFragment.definedDeclarations)
            serializeCompiledLinkerData(mainFragment.linkerData)
        }
    }
}

class WasmIrModule(
    override val moduleName: String,
    override val fragments: List<WasmCompiledCodeFileFragment>,
) : IrICModule()