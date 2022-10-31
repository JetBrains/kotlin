/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.backend.common.serialization.IdSignatureDeserializer
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.impl.javaFile
import org.jetbrains.kotlin.protobuf.CodedInputStream
import org.jetbrains.kotlin.protobuf.CodedOutputStream
import java.io.File


internal class IncrementalCache(private val library: KotlinLibrary, cachePath: String) {
    companion object {
        private const val CACHE_HEADER = "ic.header.bin"

        private const val BINARY_AST_SUFFIX = "ast.bin"
        private const val METADATA_SUFFIX = "metadata.bin"
        private const val METADATA_TMP_SUFFIX = "metadata.tmp.bin"
    }

    private var forceRebuildJs = false
    private val cacheDir = File(cachePath)
    private val signatureToIndexMappingFromMetadata = hashMapOf<KotlinSourceFile, MutableMap<IdSignature, Int>>()

    private val libraryFile = KotlinLibraryFile(library)

    class CacheHeader(val klibFileHash: ICHash = ICHash(), val configHash: ICHash = ICHash()) {
        fun toProtoStream(out: CodedOutputStream) {
            klibFileHash.toProtoStream(out)
            configHash.toProtoStream(out)
        }

        companion object {
            fun fromProtoStream(input: CodedInputStream): CacheHeader {
                val klibFileHash = ICHash.fromProtoStream(input)
                val configHash = ICHash.fromProtoStream(input)
                return CacheHeader(klibFileHash, configHash)
            }
        }
    }

    private var cacheHeader = File(cacheDir, CACHE_HEADER).useCodedInputIfExists {
        CacheHeader.fromProtoStream(this)
    } ?: CacheHeader()

    private fun loadCachedFingerprints() = File(cacheDir, CACHE_HEADER).useCodedInputIfExists {
        // skip cache header
        CacheHeader.fromProtoStream(this@useCodedInputIfExists)
        buildMapUntil(readInt32()) {
            val file = KotlinSourceFile.fromProtoStream(this@useCodedInputIfExists)
            put(file, ICHash.fromProtoStream(this@useCodedInputIfExists))
        }
    } ?: emptyMap()

    private val kotlinLibraryHeader: KotlinLibraryHeader by lazy { KotlinLibraryHeader(library) }

    private class KotlinSourceFileMetadataFromDisk(
        override val inverseDependencies: KotlinSourceFileMap<Set<IdSignature>>,
        override val directDependencies: KotlinSourceFileMap<Map<IdSignature, ICHash>>,
    ) : KotlinSourceFileMetadata()

    private object KotlinSourceFileMetadataNotExist : KotlinSourceFileMetadata() {
        override val inverseDependencies = KotlinSourceFileMap<Set<IdSignature>>(emptyMap())
        override val directDependencies = KotlinSourceFileMap<Map<IdSignature, ICHash>>(emptyMap())
    }

    private val kotlinLibrarySourceFileMetadata = hashMapOf<KotlinSourceFile, KotlinSourceFileMetadata>()

    private fun KotlinSourceFile.getCacheFile(suffix: String) = File(cacheDir, "${File(path).name}.${path.stringHashForIC()}.$suffix")

    private fun commitCacheHeader(fingerprints: List<Pair<KotlinSourceFile, ICHash>>) = File(cacheDir, CACHE_HEADER).useCodedOutput {
        cacheHeader.toProtoStream(this)
        writeInt32NoTag(fingerprints.size)
        for ((srcFile, fingerprint) in fingerprints) {
            srcFile.toProtoStream(this)
            fingerprint.toProtoStream(this)
        }
    }

    fun buildIncrementalCacheArtifact(signatureToIndexMapping: Map<KotlinSourceFile, Map<IdSignature, Int>>): IncrementalCacheArtifact {
        val fileArtifacts = kotlinLibraryHeader.sourceFiles.map { srcFile ->
            commitSourceFileMetadata(srcFile.getCacheFile(BINARY_AST_SUFFIX), srcFile, signatureToIndexMapping[srcFile] ?: emptyMap())
        }
        return IncrementalCacheArtifact(cacheDir, forceRebuildJs, fileArtifacts)
    }

    data class ModifiedFiles(
        val dirtyFiles: Map<KotlinSourceFile, KotlinSourceFileMetadata> = emptyMap(),
        val removedFiles: Map<KotlinSourceFile, KotlinSourceFileMetadata> = emptyMap(),
        val newFiles: Set<KotlinSourceFile> = emptySet(),
        val modifiedConfigFiles: Set<KotlinSourceFile> = emptySet(),
    )

    fun collectModifiedFiles(configHash: ICHash): ModifiedFiles {
        var isConfigModified = false
        val klibFileHash = library.libraryFile.javaFile().fileHashForIC()
        cacheHeader = when {
            cacheHeader.configHash != configHash -> {
                cacheDir.deleteRecursively()
                isConfigModified = cacheHeader.configHash != ICHash()
                CacheHeader(klibFileHash, configHash)
            }

            cacheHeader.klibFileHash != klibFileHash -> CacheHeader(klibFileHash, configHash)
            else -> return ModifiedFiles()
        }

        val cachedFingerprints = loadCachedFingerprints()
        val deletedFiles = HashSet(cachedFingerprints.keys)
        val unknownFiles = mutableSetOf<KotlinSourceFile>()

        val newFingerprints = kotlinLibraryHeader.sourceFiles.mapIndexed { index, file -> file to library.fingerprint(index) }
        val modifiedFiles = HashMap<KotlinSourceFile, KotlinSourceFileMetadata>(newFingerprints.size).apply {
            for ((file, fileNewFingerprint) in newFingerprints) {
                val oldFingerprint = cachedFingerprints[file]
                if (oldFingerprint == null) {
                    unknownFiles += file
                }
                if (oldFingerprint != fileNewFingerprint) {
                    val metadata = fetchSourceFileMetadata(file, false)
                    put(file, metadata)
                }
                deletedFiles.remove(file)
            }
        }

        val removedFilesMetadata = deletedFiles.associateWith {
            val metadata = fetchSourceFileMetadata(it, false)
            it.getCacheFile(BINARY_AST_SUFFIX).delete()
            it.getCacheFile(METADATA_SUFFIX).delete()
            metadata
        }

        forceRebuildJs = deletedFiles.isNotEmpty()
        commitCacheHeader(newFingerprints)

        val (newFiles, modifiedConfigFiles) = if (isConfigModified) {
            emptySet<KotlinSourceFile>() to unknownFiles
        } else {
            unknownFiles to emptySet<KotlinSourceFile>()
        }

        return ModifiedFiles(modifiedFiles, removedFilesMetadata, newFiles, modifiedConfigFiles)
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
                kotlinLibraryHeader.signatureDeserializers[srcFile] ?: notFoundIcError("signature deserializer", libraryFile, srcFile)
            }

            fun CodedInputStream.deserializeIdSignatureAndSave() = readIdSignature { index ->
                val signature = deserializer.deserializeIdSignature(index)
                signatureToIndexMapping[signature] = index
                signature
            }

            fun <T> CodedInputStream.readDependencies(signaturesReader: () -> T) = buildMapUntil(readInt32()) {
                val libraryFile = KotlinLibraryFile.fromProtoStream(this@readDependencies)
                val depends = buildMapUntil(readInt32()) {
                    val dependencySrcFile = KotlinSourceFile.fromProtoStream(this@readDependencies)
                    put(dependencySrcFile, signaturesReader())
                }
                put(libraryFile, depends)
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
