/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.backend.common.serialization.FingerprintHash
import org.jetbrains.kotlin.backend.common.serialization.cityHash64String
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.protobuf.CodedInputStream
import org.jetbrains.kotlin.protobuf.CodedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File

internal class IncrementalCache(private val library: KotlinLibraryHeader, val cacheDir: File) {
    companion object {
        private const val CACHE_HEADER = "ic.header.bin"
        private const val STUBBED_SYMBOLS = "ic.stubbed-symbols.bin"

        private const val BINARY_AST_SUFFIX = "ast.bin"
        private const val METADATA_SUFFIX = "metadata.bin"
    }

    private val cacheHeaderFile = File(cacheDir, CACHE_HEADER)
    private val stubbedSymbolsFile = File(cacheDir, STUBBED_SYMBOLS)

    private var cacheHeaderShouldBeUpdated = false

    private var removedSrcFiles: Set<KotlinSourceFile> = emptySet()
    private var modifiedSrcFiles: Set<KotlinSourceFile> = emptySet()

    private val kotlinLibrarySourceFileMetadata = hashMapOf<KotlinSourceFile, KotlinSourceFileMetadata>()

    private val idSignatureSerialization = IdSignatureSerialization(library)

    private val cacheHeaderFromDisk by lazy(LazyThreadSafetyMode.NONE) {
        cacheHeaderFile.useCodedInputIfExists {
            CacheHeader.fromProtoStream(this)
        }
    }

    private val filesWithStubbedSignatures: Map<KotlinSourceFile, Set<IdSignature>> by lazy {
        fetchFilesWithStubbedSymbols()
    }

    val libraryFileFromHeader by lazy(LazyThreadSafetyMode.NONE) { cacheHeaderFromDisk?.libraryFile }

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
        val pathHash = path.cityHash64String()
        return File(cacheDir, "${File(path).name}.$pathHash.$suffix")
    }

    fun buildAndCommitCacheArtifact(
        signatureToIndexMapping: Map<KotlinSourceFile, Map<IdSignature, Int>>,
        stubbedSignatures: Set<IdSignature>
    ): IncrementalCacheArtifact {
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

        val updatedFilesWithStubbedSignatures = hashMapOf<KotlinSourceFile, Set<IdSignature>>()

        val fileArtifacts = klibSrcFiles.map { srcFile ->
            val signatureMapping = signatureToIndexMapping[srcFile] ?: emptyMap()
            val artifact = commitSourceFileMetadata(srcFile, signatureMapping)

            val fileStubbedSignatures = when (artifact) {
                is SourceFileCacheArtifact.CommitMetadata -> signatureMapping.keys.filterTo(hashSetOf()) { it in stubbedSignatures }
                else -> filesWithStubbedSignatures[srcFile] ?: emptySet()
            }
            if (fileStubbedSignatures.isNotEmpty()) {
                updatedFilesWithStubbedSignatures[srcFile] = fileStubbedSignatures
            }
            artifact
        }

        commitFilesWithStubbedSignatures(updatedFilesWithStubbedSignatures, signatureToIndexMapping)

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
        modifiedSrcFiles = modifiedFiles.keys
        cacheHeaderShouldBeUpdated = true

        return ModifiedFiles(addedFiles, removedFiles, modifiedFiles, nonModifiedFiles)
    }

    fun fetchSourceFileFullMetadata(srcFile: KotlinSourceFile): KotlinSourceFileMetadata {
        return fetchSourceFileMetadata(srcFile, true)
    }

    fun updateSourceFileMetadata(srcFile: KotlinSourceFile, sourceFileMetadata: KotlinSourceFileMetadata) {
        kotlinLibrarySourceFileMetadata[srcFile] = sourceFileMetadata
    }

    fun collectFilesWithStubbedSignatures(): Map<KotlinSourceFile, Set<IdSignature>> {
        return filesWithStubbedSignatures
    }

    private fun fetchFilesWithStubbedSymbols(): Map<KotlinSourceFile, Set<IdSignature>> {
        return stubbedSymbolsFile.useCodedInputIfExists {
            buildMapUntil(readInt32()) {
                val srcFile = KotlinSourceFile.fromProtoStream(this@useCodedInputIfExists)
                val signatureDeserializer = idSignatureSerialization.getIdSignatureDeserializer(srcFile)
                if (srcFile in modifiedSrcFiles || srcFile in removedSrcFiles) {
                    repeat(readInt32()) {
                        signatureDeserializer.skipIdSignature(this@useCodedInputIfExists)
                    }
                } else {
                    val unboundSignatures = buildSetUntil(readInt32()) {
                        add(signatureDeserializer.deserializeIdSignature(this@useCodedInputIfExists))
                    }
                    put(srcFile, unboundSignatures)
                }
            }
        } ?: emptyMap()
    }

    private fun commitFilesWithStubbedSignatures(
        updatedFilesWithStubbedSignatures: Map<KotlinSourceFile, Set<IdSignature>>,
        signatureToIndexMapping: Map<KotlinSourceFile, Map<IdSignature, Int>>,
    ) {
        if (updatedFilesWithStubbedSignatures.isEmpty()) {
            stubbedSymbolsFile.delete()
            return
        }

        if (updatedFilesWithStubbedSignatures == filesWithStubbedSignatures) {
            return
        }

        stubbedSymbolsFile.useCodedOutput {
            writeInt32NoTag(updatedFilesWithStubbedSignatures.size)
            for ((srcFile, stubbedSignatures) in updatedFilesWithStubbedSignatures) {
                val serializer = idSignatureSerialization.getIdSignatureSerializer(srcFile, signatureToIndexMapping[srcFile] ?: emptyMap())
                srcFile.toProtoStream(this@useCodedOutput)
                writeInt32NoTag(stubbedSignatures.size)
                for (signature in stubbedSignatures) {
                    serializer.serializeIdSignature(this@useCodedOutput, signature)
                }
            }
        }
    }

    private fun fetchSourceFileMetadata(srcFile: KotlinSourceFile, loadSignatures: Boolean) =
        kotlinLibrarySourceFileMetadata.getOrPut(srcFile) {
            val deserializer = idSignatureSerialization.getIdSignatureDeserializer(srcFile)

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
                        val signature = deserializer.deserializeIdSignature(this@readDirectDependencies)
                        put(signature, ICHash.fromProtoStream(this@readDirectDependencies))
                    }
                } else {
                    repeat(readInt32()) {
                        deserializer.skipIdSignature(this@readDirectDependencies)
                        ICHash.fromProtoStream(this@readDirectDependencies)
                    }
                    emptyMap()
                }
            }

            fun CodedInputStream.readInverseDependencies() = readDependencies {
                if (loadSignatures) {
                    buildSetUntil(readInt32()) { add(deserializer.deserializeIdSignature(this@readInverseDependencies)) }
                } else {
                    repeat(readInt32()) { deserializer.skipIdSignature(this@readInverseDependencies) }
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
        val sourceFileMetadata = kotlinLibrarySourceFileMetadata[srcFile]
            ?: return SourceFileCacheArtifact.DoNotChangeMetadata(srcFile, binaryAstFile)

        val headerCacheFile = srcFile.getCacheFile(METADATA_SUFFIX)
        if (sourceFileMetadata.isEmpty()) {
            return SourceFileCacheArtifact.RemoveMetadata(srcFile, binaryAstFile, headerCacheFile)
        }
        if (sourceFileMetadata is KotlinSourceFileMetadataFromDisk) {
            return SourceFileCacheArtifact.DoNotChangeMetadata(srcFile, binaryAstFile)
        }

        val serializer = idSignatureSerialization.getIdSignatureSerializer(srcFile, signatureToIndexMapping)

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
                serializer.serializeIdSignature(this@writeDirectDependencies, signature)
                hash.toProtoStream(this)
            }
        }

        fun CodedOutputStream.writeInverseDependencies(depends: KotlinSourceFileMap<Set<IdSignature>>) = writeDependencies(depends) {
            writeInt32NoTag(it.size)
            for (signature in it) {
                serializer.serializeIdSignature(this@writeInverseDependencies, signature)
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
