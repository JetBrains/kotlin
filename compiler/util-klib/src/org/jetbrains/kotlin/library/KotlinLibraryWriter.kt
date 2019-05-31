/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.library

import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.konan.properties.Properties

interface BaseWriter {
    val versions: KonanLibraryVersioning
    fun addLinkDependencies(libraries: List<KotlinLibrary>)
    fun addManifestAddend(properties: Properties)
    fun commit()
}

interface MetadataWriter {
    fun addMetadata(metadata: SerializedMetadata)
}

interface IrWriter {
    fun addIr(ir: SerializedIr)
    fun addDataFlowGraph(dataFlowGraph: ByteArray)
}

interface KotlinLibraryWriter : MetadataWriter, BaseWriter, IrWriter

// TODO: Move SerializedIr here too to eliminate dependency on backend.common.serialization
class SerializedMetadata(
    val module: ByteArray,
    val fragments: List<List<ByteArray>>,
    val fragmentNames: List<String>
)

class SerializedIr (
    val module: ByteArray,
    val combinedDeclarationFilePath: String
)