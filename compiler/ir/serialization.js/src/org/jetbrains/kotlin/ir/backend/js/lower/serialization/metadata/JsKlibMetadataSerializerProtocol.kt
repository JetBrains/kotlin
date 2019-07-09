/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.metadata

import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol

object JsKlibMetadataSerializerProtocol : SerializerExtensionProtocol(
    ExtensionRegistryLite.newInstance().apply(JsKlibMetadataProtoBuf::registerAllExtensions),
    JsKlibMetadataProtoBuf.packageFqName,
    JsKlibMetadataProtoBuf.constructorAnnotation,
    JsKlibMetadataProtoBuf.classAnnotation,
    JsKlibMetadataProtoBuf.functionAnnotation,
    JsKlibMetadataProtoBuf.propertyAnnotation,
    JsKlibMetadataProtoBuf.propertyGetterAnnotation,
    JsKlibMetadataProtoBuf.propertySetterAnnotation,
    JsKlibMetadataProtoBuf.enumEntryAnnotation,
    JsKlibMetadataProtoBuf.compileTimeValue,
    JsKlibMetadataProtoBuf.parameterAnnotation,
    JsKlibMetadataProtoBuf.typeAnnotation,
    JsKlibMetadataProtoBuf.typeParameterAnnotation
)
