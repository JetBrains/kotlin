/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.metadata

import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol

object KlibMetadataSerializerProtocol : SerializerExtensionProtocol(
    ExtensionRegistryLite.newInstance().apply(KlibMetadataProtoBuf::registerAllExtensions),
    KlibMetadataProtoBuf.packageFqName,
    KlibMetadataProtoBuf.constructorAnnotation,
    KlibMetadataProtoBuf.classAnnotation,
    KlibMetadataProtoBuf.functionAnnotation,
    KlibMetadataProtoBuf.functionExtensionReceiverAnnotation,
    KlibMetadataProtoBuf.propertyAnnotation,
    KlibMetadataProtoBuf.propertyGetterAnnotation,
    KlibMetadataProtoBuf.propertySetterAnnotation,
    KlibMetadataProtoBuf.propertyExtensionReceiverAnnotation,
    KlibMetadataProtoBuf.propertyBackingFieldAnnotation,
    KlibMetadataProtoBuf.propertyDelegatedFieldAnnotation,
    KlibMetadataProtoBuf.enumEntryAnnotation,
    KlibMetadataProtoBuf.compileTimeValue,
    KlibMetadataProtoBuf.parameterAnnotation,
    KlibMetadataProtoBuf.typeAnnotation,
    KlibMetadataProtoBuf.typeParameterAnnotation
)
