/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.konan.file.use
import org.jetbrains.kotlin.utils.newHashSetWithExpectedSize
import java.io.BufferedOutputStream
import java.io.File

internal sealed class SourceFileCacheArtifact(val srcFile: KotlinSourceFile, val binaryAstFile: File) {
    abstract fun commitMetadata()

    fun commitBinaryAst(fragments: IrProgramFragments, icContext: PlatformDependentICContext) {
        binaryAstFile.parentFile?.mkdirs()
        BufferedOutputStream(binaryAstFile.outputStream()).use {
            fragments.serialize(it)
        }
    }

    class DoNotChangeMetadata(srcFile: KotlinSourceFile, binaryAstFile: File) : SourceFileCacheArtifact(srcFile, binaryAstFile) {
        override fun commitMetadata() {}
    }

    class CommitMetadata(
        srcFile: KotlinSourceFile,
        binaryAstFile: File,
        private val metadataFile: File,
        private val encodedMetadata: ByteArray,
    ) : SourceFileCacheArtifact(srcFile, binaryAstFile) {
        override fun commitMetadata() {
            metadataFile.parentFile?.mkdirs()
            metadataFile.writeBytes(encodedMetadata)
        }
    }

    class RemoveMetadata(
        srcFile: KotlinSourceFile,
        binaryAstFile: File,
        private val metadataFile: File,
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
    private val externalModuleName: String?,
) {
    fun getSourceFiles() = srcCacheActions.mapTo(newHashSetWithExpectedSize(srcCacheActions.size)) { it.srcFile }

    fun buildModuleArtifactAndCommitCache(
        moduleName: String,
        rebuiltFileFragments: Map<KotlinSourceFile, IrProgramFragments>,
        icContext: PlatformDependentICContext,
    ): ModuleArtifact {
        val fileArtifacts = srcCacheActions.map { srcFileAction ->
            val rebuiltFileFragment = rebuiltFileFragments[srcFileAction.srcFile]
            if (rebuiltFileFragment != null) {
                srcFileAction.commitBinaryAst(rebuiltFileFragment, icContext)
            }
            srcFileAction.commitMetadata()
            icContext.createSrcFileArtifact(srcFileAction.srcFile.path, rebuiltFileFragment, srcFileAction.binaryAstFile)
        }
        return icContext.createModuleArtifact(moduleName, fileArtifacts, artifactsDir, forceRebuildJs, externalModuleName)
    }
}
