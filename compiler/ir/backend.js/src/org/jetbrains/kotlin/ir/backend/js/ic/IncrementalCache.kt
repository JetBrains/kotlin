/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.backend.common.serialization.IdSignatureDeserializer
import org.jetbrains.kotlin.backend.common.serialization.cityHash64
import org.jetbrains.kotlin.ir.backend.web.FingerprintHash
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.protobuf.CodedInputStream
import org.jetbrains.kotlin.protobuf.CodedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File

internal class IncrementalCache(private val library: KotlinLibraryHeader, val cacheDir: File) {
    companion object {
        private const val CACHE_HEADER = "ic.header.bin"

        private const val BINARY_AST_SUFFIX = "ast.bin"
        private const val METADATA_SUFFIX = "metadata.bin"
    }

    private val cacheHeaderFile = File(cacheDir, CACHE_HEADER)

    private var cacheHeaderShouldBeUpdated = false

    private var removedSrcFiles: Collection<KotlinSourceFile> = emptyList()

    private val kotlinLibrarySourceFileMetadata = hashMapOf<KotlinSourceFile, KotlinSourceFileMetadata>()

    private val signatureToIndexMappingFromMetadata = hashMapOf<KotlinSourceFile, MutableMap<IdSignature, Int>>()

    private val cacheHeaderFromDisk by lazy {
        cacheHeaderFile.useCodedInputIfExists {
            CacheHeader.fromProtoStream(this)
        }
    }

    val libraryFileFromHeader by lazy { cacheHeaderFromDisk?.libraryFile }

    private class CacheHeader(
        val libraryFile: KotlinLibraryFile,
        val libraryFingerprint: FingerprintHash?,
        val sourceFileFingerprints: Map<KotlinSourceFile, FingerprintHash>
    ) {
        constructor(library: KotlinLibraryHeader) : this(library.libraryFile, library.libraryFingerprint, library.sourceFileFingerprints)

        fun toProtoStream(out: CodedOutputStream) {
            libraryFile.toProtoStream(out)

            libraryFingerprint?.hash?.toProtoStream(out) ?: notFoundIcError("library fingerprint", libraryFile)

            out.writeInt32NoTag(sourceFileFingerprints.size)
            for ((srcFile, fingerprint) in sourceFileFingerprints) {
                srcFile.toProtoStream(out)
                fingerprint.hash.toProtoStream(out)
            }
        }

        companion object {
            fun fromProtoStream(input: CodedInputStream): CacheHeader {
                val libraryFile = KotlinLibraryFile.fromProtoStream(input)
                val oldLibraryFingerprint = FingerprintHash(readHash128BitsFromProtoStream(input))

                val sourceFileFingerprints = buildMapUntil(input.readInt32()) {
                    val file = KotlinSourceFile.fromProtoStream(input)
                    put(file, FingerprintHash(readHash128BitsFromProtoStream(input)))
                }
                return CacheHeader(libraryFile, oldLibraryFingerprint, sourceFileFingerprints)
            }
        }
    }

    private class KotlinSourceFileMetadataFromDisk(
        override val inverseDependencies: KotlinSourceFileMap<Set<IdSignature>>,
        override val directDependencies: KotlinSourceFileMap<Map<IdSignature, ICHash>>,
    ) : KotlinSourceFileMetadata()

    private fun KotlinSourceFile.getCacheFile(suffix: String): File {
        val pathHash = path.cityHash64().toULong().toString(Character.MAX_RADIX)
        return File(cacheDir, "${File(path).name}.$pathHash.$suffix")
    }

    fun buildIncrementalCacheArtifact(signatureToIndexMapping: Map<KotlinSourceFile, Map<IdSignature, Int>>): IncrementalCacheArtifact {
        val klibSrcFiles = if (cacheHeaderShouldBeUpdated) {
            val newCacheHeader = CacheHeader(library)
            cacheHeaderFile.useCodedOutput { newCacheHeader.toProtoStream(this) }
            newCacheHeader.sourceFileFingerprints.keys
        } else {
            cacheHeaderFromDisk?.sourceFileFingerprints?.keys ?: notFoundIcError("source file fingerprints", library.libraryFile)
        }

        for (removedFile in removedSrcFiles) {
            removedFile.getCacheFile(BINARY_AST_SUFFIX).delete()
            removedFile.getCacheFile(METADATA_SUFFIX).delete()
        }

        val fileArtifacts = klibSrcFiles.map { srcFile ->
            commitSourceFileMetadata(srcFile, signatureToIndexMapping[srcFile] ?: emptyMap())
        }
        return IncrementalCacheArtifact(cacheDir, removedSrcFiles.isNotEmpty(), fileArtifacts, library.jsOutputName)
    }

    data class ModifiedFiles(
        val addedFiles: Collection<KotlinSourceFile> = emptyList(),
        val removedFiles: Map<KotlinSourceFile, KotlinSourceFileMetadata> = emptyMap(),
        val modifiedFiles: Map<KotlinSourceFile, KotlinSourceFileMetadata> = emptyMap(),
        val nonModifiedFiles: Collection<KotlinSourceFile> = emptyList()
    )

    fun collectModifiedFiles(): ModifiedFiles {
        val cachedFingerprints = cacheHeaderFromDisk?.sourceFileFingerprints ?: emptyMap()
        if (cacheHeaderFromDisk?.libraryFingerprint == library.libraryFingerprint) {
            return ModifiedFiles(emptyList(), emptyMap(), emptyMap(), cachedFingerprints.keys)
        }

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
        srcFile: KotlinSourceFile,
        signatureToIndexMapping: Map<IdSignature, Int>
    ): SourceFileCacheArtifact {
        val binaryAstFile = srcFile.getCacheFile(BINARY_AST_SUFFIX)
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

        val encodedMetadata = ByteArrayOutputStream(4096).apply {
            useCodedOutput {
                writeDirectDependencies(sourceFileMetadata.directDependencies)
                writeInverseDependencies(sourceFileMetadata.inverseDependencies)
            }
        }.toByteArray()

        return SourceFileCacheArtifact.CommitMetadata(srcFile, binaryAstFile, headerCacheFile, encodedMetadata)
    }
}
