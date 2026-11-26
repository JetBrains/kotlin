/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.KtPsiSourceFile
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibSingleFileMetadataSerializer
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.incrementalNextRoundChecker
import org.jetbrains.kotlin.incremental.js.IncrementalDataProvider
import org.jetbrains.kotlin.library.SerializedIrFile
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

typealias ProcessFun = (
    sourceFile: File,
    fileData: ByteArray,
    types: ByteArray,
    signatures: ByteArray,
    strings: ByteArray,
    declarations: ByteArray,
    bodies: ByteArray,
    fqn: ByteArray,
    debugInfo: ByteArray?,
    fileEntries: ByteArray?
) -> Unit

fun <SourceFile> shouldGoToNextIcRound(
    compilerConfiguration: CompilerConfiguration,
    createMetadataSerializer: () -> KlibSingleFileMetadataSerializer<SourceFile>,
): Boolean {
    val nextRoundChecker = compilerConfiguration.incrementalNextRoundChecker ?: return false
    createMetadataSerializer().run {
        forEachFile { _, ioFile, sourceFile, _, _ ->
            val protoBuf = serializeSingleFileMetadata(sourceFile)
            // to minimize the number of IC rounds, we should inspect all proto for changes first,
            // then go to the next round if needed, with all new dirty files
            nextRoundChecker.checkProtoChanges(ioFile, protoBuf.toByteArray())
        }
    }
    return nextRoundChecker.shouldGoToNextRound()
}

fun IncrementalDataProvider.getSerializedData(nonCompiledSources: Set<File>): List<KotlinFileSerializedData> {
    val compiledIrFiles = serializedIrFiles
    val compiledIrInlineFiles = serializedIrInlineFiles
    val compiledMetaFiles = compiledPackageParts
    val compilerSignatureIndices = serializedSignatureIndex

    assert(compiledIrFiles.size == compiledMetaFiles.size)

    val storage = mutableListOf<KotlinFileSerializedData>()

    for (f in compiledIrFiles.keys) {
        if (f in nonCompiledSources) continue

        val irData = compiledIrFiles[f] ?: error("No Ir Data found for file $f")
        val metaFile = compiledMetaFiles[f] ?: error("No Meta Data found for file $f")
        val irFile = with(irData) {
            SerializedIrFile(
                fileData,
                String(fqn),
                f.path.replace('\\', '/'),
                types,
                signatures,
                strings,
                bodies,
                declarations,
                debugInfo,
                fileEntries,
            )
        }
        val irInlineFile = compiledIrInlineFiles[f]?.run {
            SerializedIrFile(
                fileData,
                String(fqn),
                f.path.replace('\\', '/'),
                types,
                signatures,
                strings,
                bodies,
                declarations,
                debugInfo,
                fileEntries,
            )
        }
        storage.add(KotlinFileSerializedData(metaFile.metadata, irFile, irInlineFile, compilerSignatureIndices[f]?.signatureIndex))
    }
    return storage
}
