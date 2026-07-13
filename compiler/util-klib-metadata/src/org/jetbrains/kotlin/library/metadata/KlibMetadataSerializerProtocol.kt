/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.metadata

import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf.registerAllExtensions
import org.jetbrains.kotlin.metadata.ExtensionRegistryLite
import org.jetbrains.kotlin.metadata.SerializationPluginMetadataExtensions
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol

object KlibMetadataSerializerProtocol : SerializerExtensionProtocol(
    ExtensionRegistryLite {
        registerAllExtensions(this)
        SerializationPluginMetadataExtensions.registerAllExtensions(this)
    },
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
