/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ic

import org.jetbrains.kotlin.backend.wasm.ir2wasm.ModuleReferencedDeclarations
import org.jetbrains.kotlin.backend.wasm.ir2wasm.ModuleReferencedTypes
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmCompiledCodeFileFragment
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmCompiledDependencyFileFragment
import org.jetbrains.kotlin.backend.wasm.serialization.WasmSerializer
import org.jetbrains.kotlin.ir.backend.js.ic.IrICModule
import org.jetbrains.kotlin.ir.backend.js.ic.IrICProgramFragments
import java.io.OutputStream

class WasmIrProgramFragmentsMultimodule(
    mainFragment: WasmCompiledCodeFileFragment,
    val referencedTypes: ModuleReferencedTypes,
    val referencedDeclarations: ModuleReferencedDeclarations,
    val dependencyFragment: WasmCompiledDependencyFileFragment,
) : WasmIrProgramFragments(mainFragment) {
    override fun serialize(stream: OutputStream) {
        with(WasmSerializer(stream)) {
            check(mainFragment.definedTypes === dependencyFragment.definedTypes)
            serializeCompiledTypes(dependencyFragment.definedTypes)
            serializeCompiledDeclarations(dependencyFragment.definedDeclarations)
            serialize(referencedTypes)
            serialize(referencedDeclarations)
            serializeCompiledDeclarations(mainFragment.definedDeclarations)
            serializeCompiledService(mainFragment.serviceData)
        }
    }
}

open class WasmIrProgramFragments(
    override val mainFragment: WasmCompiledCodeFileFragment,
) : IrICProgramFragments() {

    override val exportFragment: WasmCompiledCodeFileFragment? = null

    override fun serialize(stream: OutputStream) {
        with(WasmSerializer(stream)) {
            serializeCompiledTypes(mainFragment.definedTypes)
            serializeCompiledDeclarations(mainFragment.definedDeclarations)
            serializeCompiledService(mainFragment.serviceData)
        }
    }
}

class WasmIrModule(
    override val moduleName: String,
    override val fragments: List<WasmCompiledCodeFileFragment>,
) : IrICModule()