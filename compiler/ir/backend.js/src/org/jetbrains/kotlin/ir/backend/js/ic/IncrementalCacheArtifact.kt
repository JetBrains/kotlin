/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsIrProgramFragment
import org.jetbrains.kotlin.ir.backend.js.utils.serialization.serializeTo
import org.jetbrains.kotlin.konan.file.use
import java.io.BufferedOutputStream
import java.io.File

internal sealed class SourceFileCacheArtifact(val srcFile: KotlinSourceFile, val binaryAstFile: File) {
    abstract fun commitMetadata()

    fun commitBinaryAst(fragment: JsIrProgramFragment) {
        binaryAstFile.recreate()
        BufferedOutputStream(binaryAstFile.outputStream()).use {
            fragment.serializeTo(it)
        }
    }

    class DoNotChangeMetadata(srcFile: KotlinSourceFile, binaryAstFile: File) : SourceFileCacheArtifact(srcFile, binaryAstFile) {
        override fun commitMetadata() {}
    }

    class CommitMetadata(
        srcFile: KotlinSourceFile,
        binaryAstFile: File,
        private val metadataFile: File,
        private val tmpMetadataFile: File
    ) : SourceFileCacheArtifact(srcFile, binaryAstFile) {
        override fun commitMetadata() {
            metadataFile.delete()
            if (!tmpMetadataFile.renameTo(metadataFile)) {
                tmpMetadataFile.copyTo(metadataFile, true)
                tmpMetadataFile.delete()
            }

        }
    }

    class RemoveMetadata(
        srcFile: KotlinSourceFile,
        binaryAstFile: File,
        private val metadataFile: File
    ) : SourceFileCacheArtifact(srcFile, binaryAstFile) {
        override fun commitMetadata() {
            metadataFile.delete()
        }
    }
}

internal class IncrementalCacheArtifact(
    private val artifactsDir: File,
    private val forceRebuildJs: Boolean,
    private val srcCacheActions: List<SourceFileCacheArtifact>,
    private val externalModuleName: String?
) {
    fun buildModuleArtifactAndCommitCache(
        moduleName: String,
        rebuiltFileFragments: Map<KotlinSourceFile, JsIrProgramFragment>,
    ): ModuleArtifact {
        val fileArtifacts = srcCacheActions.map { srcFileAction ->
            val rebuiltFileFragment = rebuiltFileFragments[srcFileAction.srcFile]
            if (rebuiltFileFragment != null) {
                srcFileAction.commitBinaryAst(rebuiltFileFragment)
            }
            srcFileAction.commitMetadata()
            SrcFileArtifact(srcFileAction.srcFile.path, rebuiltFileFragment, srcFileAction.binaryAstFile)
        }

        return ModuleArtifact(moduleName, fileArtifacts, artifactsDir, forceRebuildJs, externalModuleName)
    }
}
