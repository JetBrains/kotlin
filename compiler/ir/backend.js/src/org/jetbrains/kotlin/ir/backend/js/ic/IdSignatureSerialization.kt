/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.protobuf.CodedInputStream
import org.jetbrains.kotlin.protobuf.CodedOutputStream

private enum class IdSignatureProtoType(val id: Int) {
    DECLARED_SIGNATURE(0),
    COMMON_SIGNATURE(1),
    COMPOSITE_SIGNATURE(2),
    ACCESSOR_SIGNATURE(3);
}

internal fun CodedOutputStream.writeIdSignature(signature: IdSignature, signatureToIndexMapper: (IdSignature) -> Int?) {
    val index = signatureToIndexMapper(signature)
    if (index != null) {
        writeInt32NoTag(IdSignatureProtoType.DECLARED_SIGNATURE.id)
        writeInt32NoTag(index)
        return
    }

    when (signature) {
        is IdSignature.CommonSignature -> {
            writeInt32NoTag(IdSignatureProtoType.COMMON_SIGNATURE.id)
            writeStringNoTag(signature.packageFqName)
            writeStringNoTag(signature.declarationFqName)
            val id = signature.id
            if (id != null) {
                writeBoolNoTag(true)
                writeFixed64NoTag(id)
            } else {
                writeBoolNoTag(false)
            }
            writeInt64NoTag(signature.mask)
        }
        is IdSignature.CompositeSignature -> {
            writeInt32NoTag(IdSignatureProtoType.COMPOSITE_SIGNATURE.id)
            writeIdSignature(signature.container, signatureToIndexMapper)
            writeIdSignature(signature.inner, signatureToIndexMapper)
        }
        is IdSignature.AccessorSignature -> {
            writeInt32NoTag(IdSignatureProtoType.ACCESSOR_SIGNATURE.id)
            writeIdSignature(signature.propertySignature, signatureToIndexMapper)
            writeIdSignature(signature.accessorSignature, signatureToIndexMapper)
        }
        else -> {
            icError("can not write $signature signature")
        }
    }
}

internal fun CodedInputStream.readIdSignature(indexToSignatureMapper: (Int) -> IdSignature): IdSignature {
    when (val signatureType = readInt32()) {
        IdSignatureProtoType.DECLARED_SIGNATURE.id -> {
            return indexToSignatureMapper(readInt32())
        }
        IdSignatureProtoType.COMMON_SIGNATURE.id -> {
            val packageFqName = readString()
            val declarationFqName = readString()
            val id = if (readBool()) {
                readFixed64()
            } else {
                null
            }
            val mask = readInt64()
            return IdSignature.CommonSignature(packageFqName, declarationFqName, id, mask)
        }
        IdSignatureProtoType.COMPOSITE_SIGNATURE.id -> {
            val containerSignature = readIdSignature(indexToSignatureMapper)
            val innerSignature = readIdSignature(indexToSignatureMapper)
            return IdSignature.CompositeSignature(containerSignature, innerSignature)
        }
        IdSignatureProtoType.ACCESSOR_SIGNATURE.id -> {
            val propertySignature = readIdSignature(indexToSignatureMapper)
            val accessorSignature = readIdSignature(indexToSignatureMapper)
            if (accessorSignature !is IdSignature.CommonSignature) {
                icError("can not read accessor signature")
            }
            return IdSignature.AccessorSignature(propertySignature, accessorSignature)
        }
        else -> {
            icError("can not read signature type $signatureType")
        }
    }
}

internal fun CodedInputStream.skipIdSignature() {
    when (val signatureType = readInt32()) {
        IdSignatureProtoType.DECLARED_SIGNATURE.id -> {
            readInt32()
        }
        IdSignatureProtoType.COMMON_SIGNATURE.id -> {
            readString()
            readString()
            if (readBool()) {
                readFixed64()
            }
            readInt64()
        }
        IdSignatureProtoType.COMPOSITE_SIGNATURE.id -> {
            skipIdSignature()
            skipIdSignature()
        }
        IdSignatureProtoType.ACCESSOR_SIGNATURE.id -> {
            skipIdSignature()
            skipIdSignature()
        }
        else -> {
            icError("can not skip signature type $signatureType")
        }
    }
}
