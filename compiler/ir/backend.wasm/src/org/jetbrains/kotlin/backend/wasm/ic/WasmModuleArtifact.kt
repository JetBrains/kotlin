/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ic

import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmCompiledCodeFileFragment
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmCompiledDependencyFileFragment
import org.jetbrains.kotlin.backend.wasm.serialization.WasmDeserializer
import org.jetbrains.kotlin.ir.backend.js.ic.ModuleArtifact
import org.jetbrains.kotlin.ir.backend.js.ic.SrcFileArtifact
import java.io.File

internal inline fun <T> File.ifExists(f: File.() -> T): T? = if (exists()) f() else null

class WasmSrcFileArtifactMultimodule(
    private val compiledArtifact: WasmIrProgramFragmentsMultimodule?,
    private val astArtifact: File? = null,
    private val skipLocalNames: Boolean = false,
) : SrcFileArtifact() {

    fun loadIrDependencyFragments(): WasmCompiledDependencyFileFragment? {
        if (compiledArtifact != null) {
            return WasmCompiledDependencyFileFragment(compiledArtifact.definedTypes, compiledArtifact.dependencyDeclarations)
        }
        return astArtifact?.ifExists {
            inputStream().use {
                with(WasmDeserializer(inputStream = it, skipLocalNames = skipLocalNames)) {
                    WasmCompiledDependencyFileFragment(
                        definedTypes = deserializeCompiledTypesFragment(),
                        definedDeclarations = deserializeCompiledDeclarationsFragment(),
                    )
                }
            }
        }
    }

    override fun loadIrFragments(): WasmIrProgramFragmentsMultimodule? {
        if (compiledArtifact != null) {
            return compiledArtifact
        }

        return astArtifact?.ifExists {
            inputStream().use {
                with(WasmDeserializer(inputStream = it, skipLocalNames = skipLocalNames)) {
                    WasmIrProgramFragmentsMultimodule(
                        definedTypes = deserializeCompiledTypesFragment(),
                        dependencyDeclarations = deserializeCompiledDeclarationsFragment(),
                        referencedTypes = deserializeModuleReferencedTypes(),
                        referencedDeclarations = deserializeModuleReferencedDeclarations(),
                        codeDeclarations = deserializeCompiledDeclarationsFragment(),
                        linkerData = deserializeCompiledLinkerDataFragment(),
                    )
                }
            }
        }
    }

    override fun isModified() = compiledArtifact != null
}

class WasmSrcFileArtifact(
    private val fragments: WasmIrProgramFragments?,
    private val astArtifact: File? = null,
    private val skipLocalNames: Boolean = false,
) : SrcFileArtifact() {
    override fun loadIrFragments(): WasmIrProgramFragments? {
        if (fragments != null) {
            return fragments
        }
        return astArtifact?.ifExists {
            val fragment = inputStream().use {
                with(WasmDeserializer(inputStream = it, skipLocalNames = skipLocalNames)) {
                    WasmCompiledCodeFileFragment(
                        definedTypes = deserializeCompiledTypesFragment(),
                        definedDeclarations = deserializeCompiledDeclarationsFragment(),
                        linkerData = deserializeCompiledLinkerDataFragment(),
                    )
                }
            }
            WasmIrProgramFragments(mainFragment = fragment)
        }
    }

    override fun isModified() = fragments != null
}

class WasmModuleArtifact(
    override val fileArtifacts: List<WasmSrcFileArtifact>,
) : ModuleArtifact()

class WasmModuleArtifactMultimodule(
    override val fileArtifacts: List<WasmSrcFileArtifactMultimodule>,
    val moduleName: String,
    val externalModuleName: String?,
) : ModuleArtifact()