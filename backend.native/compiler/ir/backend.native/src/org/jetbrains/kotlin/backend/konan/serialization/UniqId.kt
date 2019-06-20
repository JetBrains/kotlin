/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.serialization.DescriptorUniqIdAware
import org.jetbrains.kotlin.backend.common.serialization.tryGetExtension
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.serialization.deserialization.descriptors.*
import org.jetbrains.kotlin.metadata.konan.KonanProtoBuf
import org.jetbrains.kotlin.protobuf.GeneratedMessageLite

object KonanDescriptorUniqIdAware: DescriptorUniqIdAware {
    override fun DeclarationDescriptor.getUniqId(): Long? = when (this) {
        is DeserializedClassDescriptor -> this.classProto.tryGetExtension(KonanProtoBuf.classUniqId)
        is DeserializedSimpleFunctionDescriptor -> this.proto.tryGetExtension(KonanProtoBuf.functionUniqId)
        is DeserializedPropertyDescriptor -> this.proto.tryGetExtension(KonanProtoBuf.propertyUniqId)
        is DeserializedClassConstructorDescriptor -> this.proto.tryGetExtension(KonanProtoBuf.constructorUniqId)
        is DeserializedTypeParameterDescriptor -> this.proto.tryGetExtension(KonanProtoBuf.typeParamUniqId)
        else -> null
    }?.index
}

fun newKonanDescriptorUniqId(index: Long): KonanProtoBuf.DescriptorUniqId =
    KonanProtoBuf.DescriptorUniqId.newBuilder().setIndex(index).build()
