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

fun newFile(fileDir: File, name: String): File {
    val f = File(fileDir, name)
    if (f.exists()) f.delete()
    return f
}

fun SerializedIcDataForFile.writeData(fileDir: File) {
    // .file
    newFile(fileDir, "file.fileData").writeBytes(file.fileData)
    newFile(fileDir, "file.path").writeBytes(file.path.toByteArray(Charsets.UTF_8))
    newFile(fileDir, "file.declarations").writeBytes(file.declarations)
    newFile(fileDir, "file.types").writeBytes(file.types)
    newFile(fileDir, "file.signatures").writeBytes(file.signatures)
    newFile(fileDir, "file.strings").writeBytes(file.strings)
    newFile(fileDir, "file.bodies").writeBytes(file.bodies)
    // .carriers
    newFile(fileDir, "carriers.signatures").writeBytes(carriers.signatures)
    newFile(fileDir, "carriers.declarationCarriers").writeBytes(carriers.declarationCarriers)
    newFile(fileDir, "carriers.bodyCarriers").writeBytes(carriers.bodyCarriers)
    newFile(fileDir, "carriers.removedOn").writeBytes(carriers.removedOn)
    // .mappings
    newFile(fileDir, "mappings.keys").writeBytes(mappings.keyBytes())
    newFile(fileDir, "mappings.values").writeBytes(mappings.valueBytes())
    // .order
    newFile(fileDir, "order.topLevelSignatures").writeBytes(order.topLevelSignatures)
    newFile(fileDir, "order.containerSignatures").writeBytes(order.containerSignatures)
    newFile(fileDir, "order.declarationSignatures").writeBytes(order.declarationSignatures)
}

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

        it.writeData(fileDir)
    }
}

private fun SerializedMappings.keyBytes() = IrMemoryArrayWriter(mappings.map { it.keys }).writeIntoMemory()
private fun SerializedMappings.valueBytes() = IrMemoryArrayWriter(mappings.map { it.values }).writeIntoMemory()

fun File.readIcDataBinary(): SerializedIcDataForFile {
    val file = SerializedIrFile(
        fileData = File(this, "file.fileData").readBytes(),
        fqName = name.split('.').dropLast(2).joinToString(separator = "."),
        path = File(this, "file.path").readBytes().toString(Charsets.UTF_8),
        types = File(this, "file.types").readBytes(),
        signatures = File(this, "file.signatures").readBytes(),
        strings = File(this, "file.strings").readBytes(),
        bodies = File(this, "file.bodies").readBytes(),
        declarations = File(this, "file.declarations").readBytes(),
        debugInfo = null
    )

    val carriers = SerializedCarriers(
        signatures = File(this, "carriers.signatures").readBytes(),
        declarationCarriers = File(this, "carriers.declarationCarriers").readBytes(),
        bodyCarriers = File(this, "carriers.bodyCarriers").readBytes(),
        removedOn = File(this, "carriers.removedOn").readBytes(),
    )

    val mappingKeys = IrArrayMemoryReader(File(this, "mappings.keys").readBytes()).toArray()
    val mappingValues = IrArrayMemoryReader(File(this, "mappings.values").readBytes()).toArray()
    assert(mappingKeys.size == mappingValues.size)
    val mappings = SerializedMappings(mappingKeys.zip(mappingValues).map { (k, v) -> SerializedMapping(k, v) })

    val order = SerializedOrder(
        topLevelSignatures = File(this, "order.topLevelSignatures").readBytes(),
        containerSignatures = File(this, "order.containerSignatures").readBytes(),
        declarationSignatures = File(this, "order.declarationSignatures").readBytes(),
    )

    return SerializedIcDataForFile(file, carriers, mappings, order)
}

fun File.readIcData(): SerializedIcData {
    if (!this.isDirectory) error("Directory doesn't exist: ${this.absolutePath}")

    return SerializedIcData(this.listFiles()!!.filter { it.isDirectory && it.name.startsWith("ic-") }.map { fileDir ->
        fileDir.readIcDataBinary()
    })
}