/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.protobuf.GeneratedMessageLite
import org.jetbrains.kotlin.serialization.deserialization.descriptors.*
import org.jetbrains.kotlin.backend.common.serialization.proto.UniqId as ProtoUniqId

// This is an abstract uniqIdIndex any serialized IR declarations gets.
// It is either isLocal and then just gets and ordinary number within its module.
// Or is visible across modules and then gets a hash of mangled name as its index.
data class UniqId(
    val index: Long,
    val isLocal: Boolean
) {
    val isPublic: Boolean get() = !isLocal
}

fun protoUniqId(uniqId: UniqId): ProtoUniqId =
    ProtoUniqId.newBuilder()
        .setIndex(uniqId.index)
        .setIsLocal(uniqId.isLocal)
        .build()

fun ProtoUniqId.uniqId(): UniqId = UniqId(this.index, this.isLocal)

fun <T, M : GeneratedMessageLite.ExtendableMessage<M>> M.tryGetExtension(extension: GeneratedMessageLite.GeneratedExtension<M, T>) =
    if (this.hasExtension(extension)) this.getExtension<T>(extension) else null

interface DescriptorUniqIdAware {
    fun DeclarationDescriptor.getUniqId(): Long?
}

object DeserializedDescriptorUniqIdAware : DescriptorUniqIdAware {
    override fun DeclarationDescriptor.getUniqId(): Long? = when (this) {
        is DeserializedClassDescriptor -> this.classProto.tryGetExtension(KlibMetadataProtoBuf.classUniqId)
        is DeserializedSimpleFunctionDescriptor -> this.proto.tryGetExtension(KlibMetadataProtoBuf.functionUniqId)
        is DeserializedPropertyDescriptor -> this.proto.tryGetExtension(KlibMetadataProtoBuf.propertyUniqId)
        is DeserializedClassConstructorDescriptor -> this.proto.tryGetExtension(KlibMetadataProtoBuf.constructorUniqId)
        is DeserializedTypeParameterDescriptor -> this.proto.tryGetExtension(KlibMetadataProtoBuf.typeParamUniqId)
        is DeserializedTypeAliasDescriptor -> this.proto.tryGetExtension(KlibMetadataProtoBuf.typeAliasUniqId)
        else -> null
    }?.index
}

fun newDescriptorUniqId(index: Long): KlibMetadataProtoBuf.DescriptorUniqId =
    KlibMetadataProtoBuf.DescriptorUniqId.newBuilder().setIndex(index).build()