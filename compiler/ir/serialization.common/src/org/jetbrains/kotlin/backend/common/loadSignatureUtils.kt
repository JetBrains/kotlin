/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.backend.common.serialization.CommonSignatureDeserializer
import org.jetbrains.kotlin.backend.common.serialization.CommonSignatureSerializer
import org.jetbrains.kotlin.backend.common.serialization.IrInterningService
import org.jetbrains.kotlin.backend.common.serialization.IrStringSerializer
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.IdSignatureRenderer
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.library.encodings.WobblyTF8
import org.jetbrains.kotlin.library.impl.IrArrayWriter
import org.jetbrains.kotlin.library.impl.IrMultiArrayReader
import org.jetbrains.kotlin.utils.newHashSetWithExpectedSize
import org.jetbrains.kotlin.backend.common.serialization.proto.CommonIdSignature as ProtoCommonIdSignature

fun serializeSignaturesIndex(
    exportedSignatures: Set<IdSignature>,
    importedSignatures: Set<IdSignature>,
): ByteArray {
    val stringSerializer = IrStringSerializer()
    val signatureSerializer = CommonSignatureSerializer(stringSerializer, debugInfoSerializer = null)

    fun serializeSignatures(unorderedSignatures: Set<IdSignature>): List<ByteArray> {
        if (unorderedSignatures.isEmpty()) return emptyList()

        val orderedSignatures = ArrayList<Pair<String, IdSignature.CommonSignature>>(unorderedSignatures.size)

        unorderedSignatures.mapTo(orderedSignatures) { signature ->
            check(signature is IdSignature.CommonSignature) {
                "Unexpected signature type: ${signature.javaClass.name}, $signature"
            }

            val sortingKey = signature.render(IdSignatureRenderer.DEFAULT)
            sortingKey to signature
        }

        orderedSignatures.sortBy { it.first }

        return orderedSignatures.map { signatureSerializer.serializeSignature(it.second).toByteArray() }
    }

    val serializedExportedSignatures = serializeSignatures(exportedSignatures)
    val serializedImportedSignatures = serializeSignatures(importedSignatures)

    return IrArrayWriter(
        listOf(
            stringSerializer.toIrStringWriter(useVarIntInDataArrays = true).writeIntoMemory(),
            IrArrayWriter(serializedExportedSignatures, useVarInt = true).writeIntoMemory(),
            IrArrayWriter(serializedImportedSignatures, useVarInt = true).writeIntoMemory(),
        ),
        useVarInt = true,
    ).writeIntoMemory()
}

fun deserializeSignaturesIndex(bytes: ByteArray): Pair<Set<IdSignature>, Set<IdSignature>> {
    val multiReader = IrMultiArrayReader(bytes)

    when (val rowCount = multiReader.rowCount()) {
        in 0..2 -> error("Unexpected row count in signature index: $rowCount")
        in 3..Int.MAX_VALUE -> {
            // OK. The number of arrays greater than the current compiler expects means we are dealing with
            // the future version of the index, which has more entries with some extra data in them.
            // We just wouldn't read that data.
        }
    }

    val stringArray = multiReader.deserializeStringArray()
    val signatureDeserializer = CommonSignatureDeserializer(
        irInterner = IrInterningService(),
        deserializeString = stringArray::get,
        deserializeDebugInfo = { null }, // There is no debug info for signatures in the signature index.
    )

    val exportedSignatures = multiReader.deserializeSignatures(rowIndex = 1, signatureDeserializer)
    val importedSignatures = multiReader.deserializeSignatures(rowIndex = 2, signatureDeserializer)

    return exportedSignatures to importedSignatures
}

private fun IrMultiArrayReader.deserializeStringArray(): Array<out String> {
    return Array(columnCount(0)) { index ->
        WobblyTF8.decode(tableItemBytes(rowIndex = 0, columnIndex = index))
    }
}

private fun IrMultiArrayReader.deserializeSignatures(
    rowIndex: Int,
    signatureDescriptors: CommonSignatureDeserializer,
): Set<IdSignature> {
    val size = columnCount(rowIndex)
    val result = newHashSetWithExpectedSize<IdSignature>(size)

    for (index in 0 until size) {
        val proto = ProtoCommonIdSignature.parseFrom(tableItemBytes(rowIndex = rowIndex, columnIndex = index))
        result += signatureDescriptors.deserializeSignature(proto)
    }

    return result
}
