/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsIrModule
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsIrProgramFragments
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.safeModuleName
import org.jetbrains.kotlin.ir.backend.js.utils.serialization.deserializeJsIrProgramFragment
import java.io.File

class SrcFileArtifact(val srcFilePath: String, private val fragments: JsIrProgramFragments?, private val astArtifact: File? = null) {
    fun loadJsIrFragments(): JsIrProgramFragments? {
        if (fragments != null) {
            return fragments
        }
        return astArtifact?.ifExists { readBytes() }?.let { deserializeJsIrProgramFragment(it) }
    }

    fun isModified() = fragments != null
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
        val fragments = fileArtifacts.sortedBy { it.srcFilePath }.flatMap {
            val fragments = it.loadJsIrFragments()
            listOfNotNull(fragments?.mainFragment, fragments?.exportFragment)
        }
        return JsIrModule(moduleSafeName, moduleExternalName, fragments)
    }
}
