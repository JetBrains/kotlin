/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library

class SerializedMetadata(
    val module: ByteArray,
    val fragments: List<List<ByteArray>>,
    val fragmentNames: List<String>,
    val metadataVersion: IntArray,
)

class SerializedFirMetadata(
    val module: ByteArray,
    val fragments: List<List<SerializedFirFile>>,
    val fragmentNames: List<String>,
    val metadataVersion: IntArray,
)

class SerializedFirFile(
    val name: String,
    val content: ByteArray,
    /**
     * The path to the source file this fragment was generated from.
     * Can be null if the fragment was generated without a corresponding source file (e.g. for generated content).
     */
    val path: String? = null,
)

class SerializedDeclaration(val id: Int, val bytes: ByteArray) {
    val size = bytes.size
}

class SerializedIrFile(
    val fileData: ByteArray,
    val fqName: String,
    val path: String,
    val types: ByteArray,
    val signatures: ByteArray,
    val strings: ByteArray,
    val bodies: ByteArray,
    val declarations: ByteArray,
    val debugInfo: ByteArray?,
    val backendSpecificMetadata: ByteArray?,
    val fileEntries: ByteArray?,
)

class SerializedIrModule(
    val files: Collection<SerializedIrFile>,
    val fileWithPreparedInlinableFunctions: SerializedIrFile?,
)
