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

abstract class SrcFileArtifact {
    abstract fun loadIrFragments(): IrProgramFragments?
    abstract fun isModified(): Boolean
}

abstract class ModuleArtifact {
    abstract val fileArtifacts: List<SrcFileArtifact>
}

/**
 * This class encapsulates the JS AST for a specific kt file, which can be either dirty or not.
 * @param srcFilePath - Path to the kt file from the klib.
 * @param fragments - The JS AST itself. It is non-null if the kt file is dirty, and its IR has been lowered and transformed into JS AST.
 * @param astArtifact - Path to a serialized JS AST. It is typically used to obtain the JS AST if the kt file is unmodified.
 */
class JsSrcFileArtifact(val srcFilePath: String, private val fragments: JsIrProgramFragments?, private val astArtifact: File? = null):
    SrcFileArtifact() {
    override fun loadIrFragments(): JsIrProgramFragments? {
        if (fragments != null) {
            return fragments
        }
        return astArtifact?.ifExists { readBytes() }?.let { deserializeJsIrProgramFragment(it) }
    }

    override fun isModified() = fragments != null
}

/**
 * This class encapsulates the JS AST for the entire klib file.
 * @param fileArtifacts - A JS AST list for each kt file in the klib.
 * @param artifactsDir - A directory where the JS AST cache is stored.
 *
 * The directory [artifactsDir] is used by both [JsPerFileCache] and [JsPerModuleCache] for storing their own caches.
 * This dirty hack allows keeping caches for [CacheUpdater] and [JsExecutableProducer] in one directory.
 */
class JsModuleArtifact(
    moduleName: String,
    override val fileArtifacts: List<JsSrcFileArtifact>,
    val artifactsDir: File? = null,
    val forceRebuildJs: Boolean = false,
    externalModuleName: String? = null
) : ModuleArtifact() {
    val moduleSafeName = moduleName.safeModuleName
    val moduleExternalName = externalModuleName ?: moduleSafeName

    fun loadJsIrModule(reexportedInModuleWithName: String? = null): JsIrModule {
        val fragments = fileArtifacts.sortedBy { it.srcFilePath }.flatMap {
            val fragments = it.loadIrFragments()
            listOfNotNull(fragments?.mainFragment, fragments?.exportFragment)
        }
        return JsIrModule(moduleSafeName, moduleExternalName, fragments, reexportedInModuleWithName)
    }
}
