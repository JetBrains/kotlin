/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ic

import org.jetbrains.kotlin.backend.wasm.ir2wasm.ModuleReferencedDeclarations
import org.jetbrains.kotlin.backend.wasm.ir2wasm.ModuleReferencedTypes
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmCompiledCodeFileFragment
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmCompiledDeclarationsFileFragment
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmCompiledLinkerDataFileFragment
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmCompiledTypesFileFragment
import org.jetbrains.kotlin.backend.wasm.serialization.WasmSerializer
import org.jetbrains.kotlin.ir.backend.js.ic.IrICModule
import org.jetbrains.kotlin.ir.backend.js.ic.IrICProgramFragments
import java.io.OutputStream

class WasmIrProgramFragmentsMultimodule(
    val definedTypes: WasmCompiledTypesFileFragment,
    val dependencyDeclarations: WasmCompiledDeclarationsFileFragment,
    val referencedTypes: ModuleReferencedTypes,
    val referencedDeclarations: ModuleReferencedDeclarations,
    val codeDeclarations: WasmCompiledDeclarationsFileFragment,
    val linkerData: WasmCompiledLinkerDataFileFragment,
) : IrICProgramFragments() {
    override fun serialize(stream: OutputStream) {
        with(WasmSerializer(stream)) {
            serializeCompiledTypes(definedTypes)
            serializeCompiledDeclarations(dependencyDeclarations)
            serialize(referencedTypes)
            serialize(referencedDeclarations)
            serializeCompiledDeclarations(codeDeclarations)
            serializeCompiledLinkerData(linkerData)
        }
    }
}

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