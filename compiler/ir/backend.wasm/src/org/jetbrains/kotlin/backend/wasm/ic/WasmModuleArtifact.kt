/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ic

import org.jetbrains.kotlin.backend.wasm.serialization.WasmDeserializer
import org.jetbrains.kotlin.ir.backend.js.ic.*
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.safeModuleName
import java.io.File

internal inline fun <T> File.ifExists(f: File.() -> T): T? = if (exists()) f() else null

class WasmSrcFileArtifact(
    private val fragments: WasmIrProgramFragments?,
    private val astArtifact: File? = null,
    private val skipLocalNames: Boolean = false,
) : SrcFileArtifact() {
    override fun loadIrFragments(): WasmIrProgramFragments? {
        if (fragments != null) {
            return fragments
        }
        return astArtifact?.ifExists { readBytes() }
            ?.let {
                WasmIrProgramFragments(
                    WasmDeserializer(
                        inputStream = it.inputStream(),
                        skipLocalNames = skipLocalNames,
                    ).deserialize()
                )
            }
    }

    override fun isModified() = fragments != null
}

class WasmModuleArtifact(
    override val fileArtifacts: List<WasmSrcFileArtifact>,
) : ModuleArtifact()