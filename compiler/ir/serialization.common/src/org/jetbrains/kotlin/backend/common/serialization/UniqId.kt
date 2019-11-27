/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.protobuf.GeneratedMessageLite
import org.jetbrains.kotlin.serialization.deserialization.descriptors.*

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