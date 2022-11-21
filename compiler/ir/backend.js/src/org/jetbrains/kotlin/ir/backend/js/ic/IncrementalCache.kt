/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.backend.common.serialization.IdSignatureDeserializer
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.protobuf.CodedInputStream
import org.jetbrains.kotlin.protobuf.CodedOutputStream
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import java.io.File

internal class IncrementalCache(private val library: KotlinLibraryHeader, val cacheDir: File) {
    companion object {
        private const val CACHE_HEADER = "ic.header.bin"

        private const val BINARY_AST_SUFFIX = "ast.bin"
        private const val METADATA_SUFFIX = "metadata.bin"
        private const val METADATA_TMP_SUFFIX = "metadata.tmp.bin"
    }

    private val cacheHeaderFile = File(cacheDir, CACHE_HEADER)

    private var cacheHeaderShouldBeUpdated = false

    private var removedSrcFiles: Collection<KotlinSourceFile> = emptyList()

    private val kotlinLibrarySourceFileMetadata = hashMapOf<KotlinSourceFile, KotlinSourceFileMetadata>()

    private val signatureToIndexMappingFromMetadata = hashMapOf<KotlinSourceFile, MutableMap<IdSignature, Int>>()

    private val cacheHeaderFromDisk by lazy {
        cacheHeaderFile.useCodedInputIfExists {
            CacheHeader.fromProtoStream(this, library.libraryFingerprint)
        }
    }

    val libraryFileFromHeader by lazy { cacheHeaderFromDisk?.libraryFile }

    private class CacheHeader(
        val libraryFile: KotlinLibraryFile,
        private val libraryFingerprint: ICHash?,
        val sourceFileFingerprints: Map<KotlinSourceFile, ICHash>?
    ) {
        constructor(library: KotlinLibraryHeader) : this(library.libraryFile, library.libraryFingerprint, library.sourceFileFingerprints)

        fun toProtoStream(out: CodedOutputStream) {
            libraryFile.toProtoStream(out)

            libraryFingerprint?.toProtoStream(out) ?: notFoundIcError("library fingerprint", libraryFile)

            sourceFileFingerprints?.let { fingerprints ->
                out.writeInt32NoTag(fingerprints.size)
                for ((srcFile, fingerprint) in fingerprints) {
                    srcFile.toProtoStream(out)
                    fingerprint.toProtoStream(out)
                }
            } ?: notFoundIcError("source file fingerprints", libraryFile)
        }

        companion object {
            fun fromProtoStream(input: CodedInputStream, newLibraryFingerprint: ICHash?): CacheHeader {
                val libraryFile = KotlinLibraryFile.fromProtoStream(input)
                val oldLibraryFingerprint = ICHash.fromProtoStream(input)

                val sourceFileFingerprints = (oldLibraryFingerprint != newLibraryFingerprint).ifTrue {
                    buildMapUntil(input.readInt32()) {
                        val file = KotlinSourceFile.fromProtoStream(input)
                        put(file, ICHash.fromProtoStream(input))
                    }
                }
                return CacheHeader(libraryFile, oldLibraryFingerprint, sourceFileFingerprints)
            }
        }
    }

    private class KotlinSourceFileMetadataFromDisk(
        override val inverseDependencies: KotlinSourceFileMap<Set<IdSignature>>,
        override val directDependencies: KotlinSourceFileMap<Map<IdSignature, ICHash>>,
    ) : KotlinSourceFileMetadata()

    private fun KotlinSourceFile.getCacheFile(suffix: String) = File(cacheDir, "${File(path).name}.${path.stringHashForIC()}.$suffix")

    fun buildIncrementalCacheArtifact(signatureToIndexMapping: Map<KotlinSourceFile, Map<IdSignature, Int>>): IncrementalCacheArtifact {
        if (cacheHeaderShouldBeUpdated) {
            cacheHeaderFile.useCodedOutput { CacheHeader(library).toProtoStream(this) }
        }

        for (removedFile in removedSrcFiles) {
            removedFile.getCacheFile(BINARY_AST_SUFFIX).delete()
            removedFile.getCacheFile(METADATA_SUFFIX).delete()
        }

        val fileArtifacts = library.sourceFileFingerprints.keys.map { srcFile ->
            commitSourceFileMetadata(srcFile.getCacheFile(BINARY_AST_SUFFIX), srcFile, signatureToIndexMapping[srcFile] ?: emptyMap())
        }
        return IncrementalCacheArtifact(cacheDir, removedSrcFiles.isNotEmpty(), fileArtifacts, library.jsOutputName)
    }

    data class ModifiedFiles(
        val addedFiles: List<KotlinSourceFile> = emptyList(),
        val removedFiles: Map<KotlinSourceFile, KotlinSourceFileMetadata> = emptyMap(),
        val modifiedFiles: Map<KotlinSourceFile, KotlinSourceFileMetadata> = emptyMap(),
        val nonModifiedFiles: List<KotlinSourceFile> = emptyList()
    )

    fun collectModifiedFiles(): ModifiedFiles {
        val cachedFingerprints = cacheHeaderFromDisk?.let { it.sourceFileFingerprints ?: return ModifiedFiles() } ?: emptyMap()

        val addedFiles = mutableListOf<KotlinSourceFile>()
        val modifiedFiles = hashMapOf<KotlinSourceFile, KotlinSourceFileMetadata>()
        val nonModifiedFiles = mutableListOf<KotlinSourceFile>()

        for ((file, fileNewFingerprint) in library.sourceFileFingerprints) {
            when (cachedFingerprints[file]) {
                fileNewFingerprint -> nonModifiedFiles.add(file)
                null -> addedFiles.add(file)
                else -> modifiedFiles[file] = fetchSourceFileMetadata(file, false)
            }
        }

        val removedFiles = (cachedFingerprints.keys - library.sourceFileFingerprints.keys).associateWith {
            fetchSourceFileMetadata(it, false)
        }

        removedSrcFiles = removedFiles.keys
        cacheHeaderShouldBeUpdated = true

        return ModifiedFiles(addedFiles, removedFiles, modifiedFiles, nonModifiedFiles)
    }

    fun fetchSourceFileFullMetadata(srcFile: KotlinSourceFile): KotlinSourceFileMetadata {
        return fetchSourceFileMetadata(srcFile, true)
    }

    fun updateSourceFileMetadata(srcFile: KotlinSourceFile, sourceFileMetadata: KotlinSourceFileMetadata) {
        kotlinLibrarySourceFileMetadata[srcFile] = sourceFileMetadata
    }

    private fun fetchSourceFileMetadata(srcFile: KotlinSourceFile, loadSignatures: Boolean) =
        kotlinLibrarySourceFileMetadata.getOrPut(srcFile) {
            val signatureToIndexMapping = signatureToIndexMappingFromMetadata.getOrPut(srcFile) { hashMapOf() }
            val deserializer: IdSignatureDeserializer by lazy {
                library.sourceFileDeserializers[srcFile] ?: notFoundIcError("signature deserializer", library.libraryFile, srcFile)
            }

            fun CodedInputStream.deserializeIdSignatureAndSave() = readIdSignature { index ->
                val signature = deserializer.deserializeIdSignature(index)
                signatureToIndexMapping[signature] = index
                signature
            }

            fun <T> CodedInputStream.readDependencies(signaturesReader: () -> T) = buildMapUntil(readInt32()) {
                val libFile = KotlinLibraryFile.fromProtoStream(this@readDependencies)
                val depends = buildMapUntil(readInt32()) {
                    val dependencySrcFile = KotlinSourceFile.fromProtoStream(this@readDependencies)
                    put(dependencySrcFile, signaturesReader())
                }
                put(libFile, depends)
            }

            fun CodedInputStream.readDirectDependencies() = readDependencies {
                if (loadSignatures) {
                    buildMapUntil(readInt32()) {
                        val signature = deserializeIdSignatureAndSave()
                        put(signature, ICHash.fromProtoStream(this@readDirectDependencies))
                    }
                } else {
                    repeat(readInt32()) {
                        skipIdSignature()
                        ICHash.fromProtoStream(this@readDirectDependencies)
                    }
                    emptyMap()
                }
            }

            fun CodedInputStream.readInverseDependencies() = readDependencies {
                if (loadSignatures) {
                    buildSetUntil(readInt32()) { add(deserializeIdSignatureAndSave()) }
                } else {
                    repeat(readInt32()) { skipIdSignature() }
                    emptySet()
                }
            }

            srcFile.getCacheFile(METADATA_SUFFIX).useCodedInputIfExists {
                val directDependencies = KotlinSourceFileMap(readDirectDependencies())
                val reverseDependencies = KotlinSourceFileMap(readInverseDependencies())
                KotlinSourceFileMetadataFromDisk(reverseDependencies, directDependencies)
            } ?: KotlinSourceFileMetadataNotExist
        }

    private fun commitSourceFileMetadata(
        binaryAstFile: File,
        srcFile: KotlinSourceFile,
        signatureToIndexMapping: Map<IdSignature, Int>
    ): SourceFileCacheArtifact {
        val headerCacheFile = srcFile.getCacheFile(METADATA_SUFFIX)
        val sourceFileMetadata = kotlinLibrarySourceFileMetadata[srcFile]
            ?: return SourceFileCacheArtifact.DoNotChangeMetadata(srcFile, binaryAstFile)
        if (sourceFileMetadata.isEmpty()) {
            return SourceFileCacheArtifact.RemoveMetadata(srcFile, binaryAstFile, headerCacheFile)
        }
        if (sourceFileMetadata is KotlinSourceFileMetadataFromDisk) {
            return SourceFileCacheArtifact.DoNotChangeMetadata(srcFile, binaryAstFile)
        }

        val signatureToIndexMappingSaved = signatureToIndexMappingFromMetadata[srcFile] ?: emptyMap()
        fun CodedOutputStream.serializeIdSignature(signature: IdSignature) =
            writeIdSignature(signature) { signatureToIndexMapping[it] ?: signatureToIndexMappingSaved[it] }

        fun <T> CodedOutputStream.writeDependencies(depends: KotlinSourceFileMap<T>, signaturesWriter: (T) -> Unit) {
            writeInt32NoTag(depends.size)
            for ((dependencyLibFile, dependencySrcFiles) in depends) {
                dependencyLibFile.toProtoStream(this)
                writeInt32NoTag(dependencySrcFiles.size)
                for ((dependencySrcFile, signatures) in dependencySrcFiles) {
                    dependencySrcFile.toProtoStream(this)
                    signaturesWriter(signatures)
                }
            }
        }

        fun CodedOutputStream.writeDirectDependencies(depends: KotlinSourceFileMap<Map<IdSignature, ICHash>>) = writeDependencies(depends) {
            writeInt32NoTag(it.size)
            for ((signature, hash) in it) {
                serializeIdSignature(signature)
                hash.toProtoStream(this)
            }
        }

        fun CodedOutputStream.writeInverseDependencies(depends: KotlinSourceFileMap<Set<IdSignature>>) = writeDependencies(depends) {
            writeInt32NoTag(it.size)
            for (signature in it) {
                serializeIdSignature(signature)
            }
        }

        val tmpCacheFile = srcFile.getCacheFile(METADATA_TMP_SUFFIX)
        tmpCacheFile.useCodedOutput {
            writeDirectDependencies(sourceFileMetadata.directDependencies)
            writeInverseDependencies(sourceFileMetadata.inverseDependencies)
        }
        return SourceFileCacheArtifact.CommitMetadata(srcFile, binaryAstFile, headerCacheFile, tmpCacheFile)
    }
}
