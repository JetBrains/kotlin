/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.konan

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.FactoryMap
import org.jetbrains.kotlin.library.metadata.KlibMetadataClassDataFinder
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.serialization.deserialization.ClassData
import org.jetbrains.kotlin.serialization.deserialization.ClassDataFinder
import org.jetbrains.kotlin.serialization.deserialization.ProtoBasedClassDataFinder

internal class NearFileClassDataFinder(
    private val delegate: ClassDataFinder,
    private val mainIndex: Int,
    private val fileCount: Int,
    private val stubFactory: (Int) -> FileWithMetadata?
) : ClassDataFinder {
    /**
     * `knm` files for commonized libraries are split to chunks.
     * In the commonizer, chunks are formed rather trivially – each chunk has a fixed number of classes (64 by default).
     * In `knm`s, nested classes are serialized separately, and nothing guarantees that they end up being in the same chunk as their
     * containing class.
     *
     * Such a quirk creates problems for stub indexing, as normally `knm` files are analyzed separately, one by one.
     * Stubs for nested classes (including one for the companion object) must be inside the containing class stub. However, class data
     * for them cannot be found with single-file [KlibMetadataClassDataFinder] (K1) or [ProtoBasedClassDataFinder] (K2) providers.
     *
     * [NearFileClassDataFinder] wraps the main [ClassDataFinder] by traversing nearby `knm` chunks. As nested classes are more likely
     * to be close to their containing class, nearby indices are traversed first.
     *
     * This is a temporary workaround until the splitting is fixed in the commonizer (KT-68202).
     *
     * See KT-65414 for the problematic case with NSString.
     * Also see `ChunkedKlibModuleFragmentWriteStrategy` and `ChunkingWriteStrategy` for chunk-splitting implementation.
     */
    companion object {
        fun wrapIfNeeded(
            delegate: ClassDataFinder,
            mainFile: VirtualFile,
            readFile: (VirtualFile) -> FileWithMetadata?,
        ): ClassDataFinder {
            val fileExtension = mainFile.extension ?: return delegate

            val nameChunks = mainFile.nameWithoutExtension.split("_", limit = 2)
            if (nameChunks.size != 2) {
                return delegate
            }

            val libraryName = nameChunks[1]
            val mainFileIndex = nameChunks[0].toIntOrNull()?.takeIf { it >= 0 } ?: return delegate

            val libraryDirectory = mainFile.parent ?: return delegate

            val fileCount = libraryDirectory.children
                .count { otherFile ->
                    otherFile.extension == fileExtension
                            // Sic! '/' is intentional here to avoid occasional matching with the empty library name (e.g. '0_.knm')
                            && otherFile.nameWithoutExtension.substringAfter("_", missingDelimiterValue = "/") == libraryName
                }

            if (fileCount < 2) {
                return delegate
            }

            return NearFileClassDataFinder(delegate, mainFileIndex, fileCount) { index ->
                computeFileNameCandidates(index, libraryName, fileExtension)
                    .firstNotNullOfOrNull { libraryDirectory.findChild(it)?.let(readFile) }
            }
        }

        private fun computeFileNameCandidates(index: Int, libraryName: String, fileExtension: String): List<String> {
            fun renderName(indexString: String) = "${indexString}_$libraryName.$fileExtension"

            return buildList {
                add(renderName(index.toString()))
                if (index < 10) {
                    add(renderName("0$index"))
                }
            }
        }
    }

    private val nearFiles: Map<Int, ProtoBasedClassDataFinder> = FactoryMap.create { index ->
        val fileWithMetadata = stubFactory(index) as? FileWithMetadata.Compatible ?: return@create null
        ProtoBasedClassDataFinder(fileWithMetadata.proto, fileWithMetadata.nameResolver, fileWithMetadata.version)
    }

    override fun findClassData(classId: ClassId): ClassData? {
        delegate.findClassData(classId)?.let { return it }

        var offset = 1

        while (true) {
            val precedingIndex = mainIndex - offset
            val followingIndex = mainIndex + offset

            var isHandled = false

            if (precedingIndex >= 0) {
                isHandled = true
                nearFiles[precedingIndex]?.findClassData(classId)?.let { return it }
            }

            if (followingIndex < fileCount) {
                isHandled = true
                nearFiles[followingIndex]?.findClassData(classId)?.let { return it }
            }

            if (!isHandled) {
                // We got out of range
                break
            }

            offset += 1
        }

        return null
    }
}