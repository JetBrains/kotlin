/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFile
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.components.irOrFail
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import java.io.File

/**
 * This interface represents the abstract klib and is mainly used for detecting modified files.
 */
internal interface KotlinLibraryHeader {
    val libraryFile: KotlinLibraryFile

    val libraryFingerprint: FingerprintHash?

    val sourceFileDeserializers: Map<KotlinSourceFile, IdSignatureDeserializer>
    val sourceFileFingerprints: Map<KotlinSourceFile, FingerprintHash>

    val jsOutputName: String?
}

/**
 * This implementation represents the existing klib that is present on the disk.
 */
internal class KotlinLoadedLibraryHeader(
    private val library: KotlinLibrary,
    private val irInterner: IrInterningService
) : KotlinLibraryHeader {
    private fun parseFingerprintsFromManifest(): Map<KotlinSourceFile, FingerprintHash>? {
        val manifestFingerprints = library.serializedIrFileFingerprints?.takeIf { it.size == sourceFiles.size } ?: return null
        return sourceFiles.withIndex().associate { it.value to manifestFingerprints[it.index].fileFingerprint }
    }

    override val libraryFile: KotlinLibraryFile = KotlinLibraryFile(library)

    override val libraryFingerprint: FingerprintHash by lazy(LazyThreadSafetyMode.NONE) {
        val serializedKlib = library.serializedKlibFingerprint ?: SerializedKlibFingerprint(File(libraryFile.path))
        serializedKlib.klibFingerprint
    }

    override val sourceFileDeserializers: Map<KotlinSourceFile, IdSignatureDeserializer> by lazy(LazyThreadSafetyMode.NONE) {
        val ir = library.irOrFail
        buildMapUntil(sourceFiles.size) {
            val deserializer = IdSignatureDeserializer(IrLibraryFileFromBytes(object : IrLibraryBytesSource() {
                private fun err(): Nothing = icError("Not supported")
                override fun irDeclaration(index: Int): ByteArray = err()
                override fun type(index: Int): ByteArray = err()
                override fun signature(index: Int): ByteArray = ir.signature(index, it)
                override fun string(index: Int): ByteArray = ir.stringLiteral(index, it)
                override fun body(index: Int): ByteArray = err()
                override fun debugInfo(index: Int): ByteArray? = null
                override fun fileEntry(index: Int): ByteArray? = ir.irFileEntry(index, it)
            }), null, irInterner)

            put(sourceFiles[it], deserializer)
        }
    }

    override val sourceFileFingerprints: Map<KotlinSourceFile, FingerprintHash> by lazy(LazyThreadSafetyMode.NONE) {
        parseFingerprintsFromManifest() ?: buildMapUntil(sourceFiles.size) {
            put(sourceFiles[it], SerializedIrFileFingerprint(library, it).fileFingerprint)
        }
    }

    override val jsOutputName: String?
        get() = library.jsOutputName

    private val sourceFiles by lazy(LazyThreadSafetyMode.NONE) {
        val extReg = ExtensionRegistryLite.newInstance()
        val ir = library.irOrFail
        val sources = (0 until ir.irFileCount).map {
            val fileProto = IrFile.parseFrom(ir.irFile(it).codedInputStream, extReg)
            val fileReader = IrLibraryFileFromBytes(IrKlibBytesSource(ir, it))
            val fileEntry = fileReader.fileEntry(fileProto)
            fileReader.deserializeFileEntryName(fileEntry)
        }
        KotlinSourceFile.fromSources(sources)
    }
}

/**
 * This implementation represents the removed klib, which no longer exists.
 * Its main aim is to correctly handle the removed files; for example, we must invalidate all reverse dependencies.
 */
internal class KotlinRemovedLibraryHeader(private val libCacheDir: File) : KotlinLibraryHeader {
    override val libraryFile: KotlinLibraryFile
        get() = icError("removed library name is unavailable; cache dir: ${libCacheDir.absolutePath}")

    override val libraryFingerprint: FingerprintHash? get() = null

    override val sourceFileDeserializers: Map<KotlinSourceFile, IdSignatureDeserializer> get() = emptyMap()
    override val sourceFileFingerprints: Map<KotlinSourceFile, FingerprintHash> get() = emptyMap()

    override val jsOutputName: String? get() = null
}
