/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.backend.common.serialization.DescriptorUniqIdAware
import org.jetbrains.kotlin.backend.common.serialization.tryGetExtension
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.metadata.JsKlibMetadataProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassConstructorDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor

object JsDescriptorUniqIdAware: DescriptorUniqIdAware {
    override fun DeclarationDescriptor.getUniqId(): Long? = when (this) {
        is DeserializedClassDescriptor -> this.classProto.tryGetExtension(JsKlibMetadataProtoBuf.classUniqId)
        is DeserializedSimpleFunctionDescriptor -> this.proto.tryGetExtension(JsKlibMetadataProtoBuf.functionUniqId)
        is DeserializedPropertyDescriptor -> this.proto.tryGetExtension(JsKlibMetadataProtoBuf.propertyUniqId)
        is DeserializedClassConstructorDescriptor -> this.proto.tryGetExtension(JsKlibMetadataProtoBuf.constructorUniqId)
        else -> null
    }?.index
}

fun newJsDescriptorUniqId(index: Long): JsKlibMetadataProtoBuf.DescriptorUniqId =
    JsKlibMetadataProtoBuf.DescriptorUniqId.newBuilder().setIndex(index).build()
