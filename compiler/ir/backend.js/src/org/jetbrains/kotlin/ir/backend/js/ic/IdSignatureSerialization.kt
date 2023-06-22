/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.protobuf.CodedInputStream
import org.jetbrains.kotlin.protobuf.CodedOutputStream

internal class IdSignatureSerialization(private val library: KotlinLibraryHeader) {
    private enum class IdSignatureProtoType(val id: Int) {
        DECLARED_SIGNATURE(0),
        COMMON_SIGNATURE(1),
        COMPOSITE_SIGNATURE(2),
        ACCESSOR_SIGNATURE(3);
    }

    interface IdSignatureICSerializer {
        fun serializeIdSignature(out: CodedOutputStream, signature: IdSignature)
    }

    interface IdSignatureICDeserializer {
        fun deserializeIdSignature(input: CodedInputStream): IdSignature
        fun skipIdSignature(input: CodedInputStream)
    }

    inner class FileIdSignatureSerialization(srcFile: KotlinSourceFile) : IdSignatureICSerializer, IdSignatureICDeserializer {
        private val deserializer by lazy {
            library.sourceFileDeserializers[srcFile] ?: notFoundIcError("signature deserializer", library.libraryFile, srcFile)
        }

        internal val signatureToIndexMapping = hashMapOf<IdSignature, Int>()

        override fun serializeIdSignature(out: CodedOutputStream, signature: IdSignature) {
            val index = signatureToIndexMapping[signature]
            if (index != null) {
                out.writeInt32NoTag(IdSignatureProtoType.DECLARED_SIGNATURE.id)
                out.writeInt32NoTag(index)
                return
            }

            when (signature) {
                is IdSignature.CommonSignature -> {
                    out.writeInt32NoTag(IdSignatureProtoType.COMMON_SIGNATURE.id)
                    out.writeStringNoTag(signature.packageFqName)
                    out.writeStringNoTag(signature.declarationFqName)
                    out.ifNotNull(signature.id, out::writeFixed64NoTag)
                    out.writeInt64NoTag(signature.mask)
                    out.ifNotNull(signature.description, out::writeStringNoTag)
                }
                is IdSignature.CompositeSignature -> {
                    out.writeInt32NoTag(IdSignatureProtoType.COMPOSITE_SIGNATURE.id)
                    serializeIdSignature(out, signature.container)
                    serializeIdSignature(out, signature.inner)
                }
                is IdSignature.AccessorSignature -> {
                    out.writeInt32NoTag(IdSignatureProtoType.ACCESSOR_SIGNATURE.id)
                    serializeIdSignature(out, signature.propertySignature)
                    serializeIdSignature(out, signature.accessorSignature)
                }
                else -> {
                    icError("can not write $signature signature")
                }
            }
        }

        override fun deserializeIdSignature(input: CodedInputStream): IdSignature {
            when (val signatureType = input.readInt32()) {
                IdSignatureProtoType.DECLARED_SIGNATURE.id -> {
                    val index = input.readInt32()
                    val signature = deserializer.deserializeIdSignature(index)
                    signatureToIndexMapping[signature] = index
                    return signature
                }
                IdSignatureProtoType.COMMON_SIGNATURE.id -> {
                    val packageFqName = input.readString()
                    val declarationFqName = input.readString()
                    val id = input.ifTrue(input::readFixed64)
                    val mask = input.readInt64()
                    val description = input.ifTrue(input::readString)
                    return IdSignature.CommonSignature(
                        packageFqName = packageFqName,
                        declarationFqName = declarationFqName,
                        id = id,
                        mask = mask,
                        description = description,
                    )
                }
                IdSignatureProtoType.COMPOSITE_SIGNATURE.id -> {
                    val containerSignature = deserializeIdSignature(input)
                    val innerSignature = deserializeIdSignature(input)
                    return IdSignature.CompositeSignature(containerSignature, innerSignature)
                }
                IdSignatureProtoType.ACCESSOR_SIGNATURE.id -> {
                    val propertySignature = deserializeIdSignature(input)
                    val accessorSignature = deserializeIdSignature(input)
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

        override fun skipIdSignature(input: CodedInputStream) {
            when (val signatureType = input.readInt32()) {
                IdSignatureProtoType.DECLARED_SIGNATURE.id -> {
                    input.readInt32()
                }
                IdSignatureProtoType.COMMON_SIGNATURE.id -> {
                    input.readString()
                    input.readString()
                    input.ifTrue(input::readFixed64)
                    input.readInt64()
                    input.ifTrue(input::readString)
                }
                IdSignatureProtoType.COMPOSITE_SIGNATURE.id -> {
                    skipIdSignature(input)
                    skipIdSignature(input)
                }
                IdSignatureProtoType.ACCESSOR_SIGNATURE.id -> {
                    skipIdSignature(input)
                    skipIdSignature(input)
                }
                else -> {
                    icError("can not skip signature type $signatureType")
                }
            }
        }
    }

    private val fileSerializers = hashMapOf<KotlinSourceFile, FileIdSignatureSerialization>()

    fun getIdSignatureDeserializer(srcFile: KotlinSourceFile): IdSignatureICDeserializer {
        return fileSerializers.getOrPut(srcFile) { FileIdSignatureSerialization(srcFile) }
    }

    fun getIdSignatureSerializer(srcFile: KotlinSourceFile, signatureToIndexMapping: Map<IdSignature, Int>): IdSignatureICSerializer {
        return fileSerializers.getOrPut(srcFile) {
            FileIdSignatureSerialization(srcFile)
        }.also { it.signatureToIndexMapping.putAll(signatureToIndexMapping) }
    }
}
