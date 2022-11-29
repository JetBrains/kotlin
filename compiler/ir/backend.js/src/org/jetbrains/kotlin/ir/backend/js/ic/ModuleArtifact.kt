/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsIrModule
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsIrProgramFragment
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.safeModuleName
import org.jetbrains.kotlin.ir.backend.js.utils.serialization.JsIrAstDeserializer
import java.io.ByteArrayInputStream
import java.io.File

class SrcFileArtifact(val srcFilePath: String, private val fragment: JsIrProgramFragment?, private val astArtifact: File? = null) {
    fun loadJsIrFragment(deserializer: JsIrAstDeserializer): JsIrProgramFragment? {
        if (fragment != null) {
            return fragment
        }
        return astArtifact?.ifExists { readBytes() }?.let {
            ByteArrayInputStream(it).use { byteStream ->
                deserializer.deserialize(byteStream)
            }
        }
    }

    fun isModified() = fragment != null
}

class ModuleArtifact(
    moduleName: String,
    val fileArtifacts: List<SrcFileArtifact>,
    val artifactsDir: File? = null,
    val forceRebuildJs: Boolean = false,
    externalModuleName: String? = null
) {
    val moduleSafeName = moduleName.safeModuleName
    val moduleExternalName = externalModuleName ?: moduleSafeName

    fun loadJsIrModule(): JsIrModule {
        val deserializer = JsIrAstDeserializer()
        val fragments = fileArtifacts.sortedBy { it.srcFilePath }.mapNotNull { it.loadJsIrFragment(deserializer) }
        return JsIrModule(moduleSafeName, moduleExternalName, fragments)
    }
}
