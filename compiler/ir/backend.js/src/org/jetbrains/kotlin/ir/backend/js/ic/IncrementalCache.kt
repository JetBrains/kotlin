/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.backend.common.serialization.IdSignatureDeserializer
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsIrFragmentAndBinaryAst
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.impl.javaFile
import org.jetbrains.kotlin.protobuf.CodedInputStream
import org.jetbrains.kotlin.protobuf.CodedOutputStream
import java.io.File


class IncrementalCache(private val library: KotlinLibrary, cachePath: String) {
    companion object {
        private const val CACHE_HEADER = "ic.header.bin"

        private const val BINARY_AST_SUFFIX = "ast.bin"
        private const val METADATA_SUFFIX = "metadata.bin"
    }

    private var forceRebuildJs = false
    private val cacheDir = File(cachePath)
    private val signatureToIndexMappingFromMetadata = mutableMapOf<KotlinSourceFile, MutableMap<IdSignature, Int>>()

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
        override val directDependencies: KotlinSourceFileMap<Set<IdSignature>>,

        override val importedInlineFunctions: Map<IdSignature, ICHash>
    ) : KotlinSourceFileMetadata()

    private object KotlinSourceFileMetadataNotExist : KotlinSourceFileMetadata() {
        override val inverseDependencies = KotlinSourceFileMap<Set<IdSignature>>(emptyMap())
        override val directDependencies = KotlinSourceFileMap<Set<IdSignature>>(emptyMap())

        override val importedInlineFunctions = emptyMap<IdSignature, ICHash>()
    }

    private val kotlinLibrarySourceFileMetadata = mutableMapOf<KotlinSourceFile, KotlinSourceFileMetadata>()

    private fun KotlinSourceFile.getCacheFile(suffix: String) = File(cacheDir, "${File(path).name}.${path.stringHashForIC()}.$suffix")

    private fun commitCacheHeader(fingerprints: List<Pair<KotlinSourceFile, ICHash>>) = File(cacheDir, CACHE_HEADER).useCodedOutput {
        cacheHeader.toProtoStream(this)
        writeInt32NoTag(fingerprints.size)
        for ((srcFile, fingerprint) in fingerprints) {
            srcFile.toProtoStream(this)
            fingerprint.toProtoStream(this)
        }
    }

    fun buildModuleArtifactAndCommitCache(
        moduleName: String,
        rebuiltFileFragments: Map<KotlinSourceFile, JsIrFragmentAndBinaryAst>,
        signatureToIndexMapping: Map<KotlinSourceFile, Map<IdSignature, Int>>
    ): ModuleArtifact {
        val fileArtifacts = kotlinLibraryHeader.sourceFiles.map { srcFile ->
            val binaryAstFile = srcFile.getCacheFile(BINARY_AST_SUFFIX)
            val rebuiltFileFragment = rebuiltFileFragments[srcFile]
            if (rebuiltFileFragment != null) {
                binaryAstFile.apply { recreate() }.writeBytes(rebuiltFileFragment.binaryAst)
            }

            commitSourceFileMetadata(srcFile, signatureToIndexMapping[srcFile] ?: emptyMap())
            SrcFileArtifact(srcFile.path, rebuiltFileFragment?.fragment, binaryAstFile)
        }

        return ModuleArtifact(moduleName, fileArtifacts, cacheDir, forceRebuildJs)
    }

    data class ModifiedFiles(
        val modified: Map<KotlinSourceFile, KotlinSourceFileMetadata> = emptyMap(),
        val removed: Map<KotlinSourceFile, KotlinSourceFileMetadata> = emptyMap(),
        val newFiles: Set<KotlinSourceFile> = emptySet()
    )

    fun collectModifiedFiles(configHash: ICHash): ModifiedFiles {
        val klibFileHash = library.libraryFile.javaFile().fileHashForIC()
        cacheHeader = when {
            cacheHeader.configHash != configHash -> {
                cacheDir.deleteRecursively()
                CacheHeader(klibFileHash, configHash)
            }
            cacheHeader.klibFileHash != klibFileHash -> CacheHeader(klibFileHash, configHash)
            else -> return ModifiedFiles()
        }

        val cachedFingerprints = loadCachedFingerprints()
        val deletedFiles = cachedFingerprints.keys.toMutableSet()
        val newFiles = mutableSetOf<KotlinSourceFile>()

        val newFingerprints = kotlinLibraryHeader.sourceFiles.mapIndexed { index, file -> file to library.fingerprint(index) }
        val modifiedFiles = buildMap(newFingerprints.size) {
            for ((file, fileNewFingerprint) in newFingerprints) {
                val oldFingerprint = cachedFingerprints[file]
                if (oldFingerprint == null) {
                    newFiles += file
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

        return ModifiedFiles(modifiedFiles, removedFilesMetadata, newFiles)
    }

    fun fetchSourceFileFullMetadata(srcFile: KotlinSourceFile): KotlinSourceFileMetadata {
        return fetchSourceFileMetadata(srcFile, true)
    }

    fun updateSourceFileMetadata(srcFile: KotlinSourceFile, sourceFileMetadata: KotlinSourceFileMetadata) {
        kotlinLibrarySourceFileMetadata[srcFile] = sourceFileMetadata
    }

    private fun fetchSourceFileMetadata(srcFile: KotlinSourceFile, loadSignatures: Boolean) =
        kotlinLibrarySourceFileMetadata.getOrPut(srcFile) {
            val signatureToIndexMapping = signatureToIndexMappingFromMetadata.getOrPut(srcFile) { mutableMapOf() }
            fun CodedInputStream.deserializeIdSignatureAndSave(deserializer: IdSignatureDeserializer) = readIdSignature { index ->
                val signature = deserializer.deserializeIdSignature(index)
                signatureToIndexMapping[signature] = index
                signature
            }

            val deserializer: IdSignatureDeserializer by lazy {
                kotlinLibraryHeader.signatureDeserializers[srcFile] ?: notFoundIcError("signature deserializer", libraryFile, srcFile)
            }

            srcFile.getCacheFile(METADATA_SUFFIX).useCodedInputIfExists {
                fun readDependencies() = buildMapUntil(readInt32()) {
                    val libraryFile = KotlinLibraryFile.fromProtoStream(this@useCodedInputIfExists)
                    val depends = buildMapUntil(readInt32()) {
                        val dependencySrcFile = KotlinSourceFile.fromProtoStream(this@useCodedInputIfExists)
                        val dependencySignatures = if (loadSignatures) {
                            buildSetUntil(readInt32()) { add(deserializeIdSignatureAndSave(deserializer)) }
                        } else {
                            repeat(readInt32()) { skipIdSignature() }
                            emptySet()
                        }
                        put(dependencySrcFile, dependencySignatures)
                    }
                    put(libraryFile, depends)
                }

                val directDependencies = KotlinSourceFileMap(readDependencies())
                val reverseDependencies = KotlinSourceFileMap(readDependencies())

                val importedInlineFunctions = if (loadSignatures) {
                    buildMapUntil(readInt32()) {
                        val signature = deserializeIdSignatureAndSave(deserializer)
                        val transitiveHash = ICHash.fromProtoStream(this@useCodedInputIfExists)
                        put(signature, transitiveHash)
                    }
                } else {
                    emptyMap()
                }

                KotlinSourceFileMetadataFromDisk(reverseDependencies, directDependencies, importedInlineFunctions)
            } ?: KotlinSourceFileMetadataNotExist
        }

    private fun commitSourceFileMetadata(srcFile: KotlinSourceFile, signatureToIndexMapping: Map<IdSignature, Int>) {
        val headerCacheFile = srcFile.getCacheFile(METADATA_SUFFIX)
        val sourceFileMetadata = kotlinLibrarySourceFileMetadata[srcFile] ?: notFoundIcError("metadata", libraryFile, srcFile)
        if (sourceFileMetadata.isEmpty()) {
            headerCacheFile.delete()
            return
        }
        if (sourceFileMetadata is KotlinSourceFileMetadataFromDisk) {
            return
        }

        val signatureToIndexMappingSaved = signatureToIndexMappingFromMetadata[srcFile] ?: emptyMap()
        fun CodedOutputStream.serializeIdSignature(signature: IdSignature) =
            writeIdSignature(signature) { signatureToIndexMapping[it] ?: signatureToIndexMappingSaved[it] }

        headerCacheFile.useCodedOutput {
            fun writeDepends(depends: KotlinSourceFileMap<Set<IdSignature>>) {
                writeInt32NoTag(depends.size)
                for ((dependencyLibFile, dependencySrcFiles) in depends) {
                    dependencyLibFile.toProtoStream(this)
                    writeInt32NoTag(dependencySrcFiles.size)
                    for ((dependencySrcFile, signatures) in dependencySrcFiles) {
                        dependencySrcFile.toProtoStream(this)
                        writeInt32NoTag(signatures.size)
                        for (signature in signatures) {
                            serializeIdSignature(signature)
                        }
                    }
                }
            }

            writeDepends(sourceFileMetadata.directDependencies)
            writeDepends(sourceFileMetadata.inverseDependencies)

            writeInt32NoTag(sourceFileMetadata.importedInlineFunctions.size)
            for ((signature, transitiveHash) in sourceFileMetadata.importedInlineFunctions) {
                serializeIdSignature(signature)
                transitiveHash.toProtoStream(this)
            }
        }
    }
}
