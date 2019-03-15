/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.metadata

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
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
) {
    fun getKjsmFilePath(packageFqName: FqName): String {
        val shortName = if (packageFqName.isRoot) Name.identifier("root-package") else packageFqName.shortName()

        return packageFqName.child(shortName).asString().replace('.', '/') +
                "." +
                JsKlibMetadataSerializationUtil.CLASS_METADATA_FILE_EXTENSION
    }
}
