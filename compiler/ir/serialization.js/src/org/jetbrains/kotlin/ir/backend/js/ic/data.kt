/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.ir.backend.js.SerializedMapping
import org.jetbrains.kotlin.ir.backend.js.SerializedMappings
import org.jetbrains.kotlin.ir.serialization.SerializedCarriers
import org.jetbrains.kotlin.library.SerializedIrFile
import org.jetbrains.kotlin.library.impl.IrArrayMemoryReader
import org.jetbrains.kotlin.library.impl.IrMemoryArrayWriter
import org.jetbrains.kotlin.library.impl.toArray
import java.io.File
import java.nio.charset.Charset

class SerializedIcData(
    val files: Collection<SerializedIcDataForFile>,
)

class SerializedIcDataForFile(
    val file: SerializedIrFile,
    val carriers: SerializedCarriers,
    val mappings: SerializedMappings,
    val order: SerializedOrder,
)

class SerializedOrder(
    val topLevelSignatures: ByteArray,
    val containerSignatures: ByteArray,
    val declarationSignatures: ByteArray,
)

fun SerializedIcData.writeTo(dir: File) {
    if (!dir.exists()) error("Directory doesn't exist: ${dir.absolutePath}")
    if (!dir.isDirectory) error("Not a directory: ${dir.absolutePath}")

    files.forEach {
        val fqnPath = it.file.fqName
        val fileId = it.file.path.hashCode().toString(Character.MAX_RADIX)
        val irFileDirectory = "ic-$fqnPath.$fileId.file"
        val fileDir = File(dir, irFileDirectory)

        // TODO: just rewrite?
        if (!fileDir.exists()) {
            if (!fileDir.mkdirs()) error("Failed to create output dir for file ${fileDir.absolutePath}")
        }

        // .file
        File(fileDir, "file.fileData").writeBytes(it.file.fileData)
        File(fileDir, "file.path").writeBytes(it.file.path.toByteArray(Charsets.UTF_8))
        File(fileDir, "file.declarations").writeBytes(it.file.declarations)
        File(fileDir, "file.types").writeBytes(it.file.types)
        File(fileDir, "file.signatures").writeBytes(it.file.signatures)
        File(fileDir, "file.strings").writeBytes(it.file.strings)
        File(fileDir, "file.bodies").writeBytes(it.file.bodies)
        // .carriers
        File(fileDir, "carriers.signatures").writeBytes(it.carriers.signatures)
        File(fileDir, "carriers.declarationCarriers").writeBytes(it.carriers.declarationCarriers)
        File(fileDir, "carriers.bodyCarriers").writeBytes(it.carriers.bodyCarriers)
        File(fileDir, "carriers.removedOn").writeBytes(it.carriers.removedOn)
        // .mappings
        File(fileDir, "mappings.keys").writeBytes(it.mappings.keyBytes())
        File(fileDir, "mappings.values").writeBytes(it.mappings.valueBytes())
        // .order
        File(fileDir, "order.topLevelSignatures").writeBytes(it.order.topLevelSignatures)
        File(fileDir, "order.containerSignatures").writeBytes(it.order.containerSignatures)
        File(fileDir, "order.declarationSignatures").writeBytes(it.order.declarationSignatures)
    }
}

private fun SerializedMappings.keyBytes() = IrMemoryArrayWriter(mappings.map { it.keys }).writeIntoMemory()
private fun SerializedMappings.valueBytes() = IrMemoryArrayWriter(mappings.map { it.values }).writeIntoMemory()

fun File.readIcData(): SerializedIcData {
    if (!this.isDirectory) error("Directory doesn't exist: ${this.absolutePath}")

    return SerializedIcData(this.listFiles()!!.filter { it.isDirectory}.map { fileDir ->
        val file = SerializedIrFile(
            fileData = File(fileDir, "file.fileData").readBytes(),
            fqName = fileDir.name.split('.').dropLast(2).joinToString(separator = "."),
            path = File(fileDir, "file.path").readBytes().toString(Charsets.UTF_8),
            types = File(fileDir, "file.types").readBytes(),
            signatures = File(fileDir, "file.signatures").readBytes(),
            strings = File(fileDir, "file.strings").readBytes(),
            bodies = File(fileDir, "file.bodies").readBytes(),
            declarations = File(fileDir, "file.declarations").readBytes()
        )

        val carriers = SerializedCarriers(
            signatures = File(fileDir, "carriers.signatures").readBytes(),
            declarationCarriers = File(fileDir, "carriers.declarationCarriers").readBytes(),
            bodyCarriers = File(fileDir, "carriers.bodyCarriers").readBytes(),
            removedOn = File(fileDir, "carriers.removedOn").readBytes(),
        )

        val mappingKeys = IrArrayMemoryReader(File(fileDir, "mappings.keys").readBytes()).toArray()
        val mappingValues = IrArrayMemoryReader(File(fileDir, "mappings.values").readBytes()).toArray()
        assert(mappingKeys.size == mappingValues.size)
        val mappings = SerializedMappings(mappingKeys.zip(mappingValues).map { (k, v) -> SerializedMapping(k, v) })

        val order = SerializedOrder(
            topLevelSignatures = File(fileDir, "order.topLevelSignatures").readBytes(),
            containerSignatures = File(fileDir, "order.containerSignatures").readBytes(),
            declarationSignatures = File(fileDir, "order.declarationSignatures").readBytes(),
        )

        SerializedIcDataForFile(file, carriers, mappings, order)
    })
}