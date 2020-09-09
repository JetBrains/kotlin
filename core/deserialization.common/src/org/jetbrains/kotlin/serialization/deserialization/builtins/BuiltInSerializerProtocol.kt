/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.deserialization.builtins

import org.jetbrains.kotlin.metadata.builtins.BuiltInsProtoBuf
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol

object BuiltInSerializerProtocol : SerializerExtensionProtocol(
    ExtensionRegistryLite.newInstance().apply(BuiltInsProtoBuf::registerAllExtensions),
    BuiltInsProtoBuf.packageFqName,
    BuiltInsProtoBuf.constructorAnnotation,
    BuiltInsProtoBuf.classAnnotation,
    BuiltInsProtoBuf.functionAnnotation,
    BuiltInsProtoBuf.propertyAnnotation,
    BuiltInsProtoBuf.propertyGetterAnnotation,
    BuiltInsProtoBuf.propertySetterAnnotation,
    BuiltInsProtoBuf.enumEntryAnnotation,
    BuiltInsProtoBuf.compileTimeValue,
    BuiltInsProtoBuf.parameterAnnotation,
    BuiltInsProtoBuf.typeAnnotation,
    BuiltInsProtoBuf.typeParameterAnnotation
) {
    const val BUILTINS_FILE_EXTENSION = "kotlin_builtins"

    fun getBuiltInsFilePath(fqName: FqName): String =
        fqName.asString().replace('.', '/') + "/" + getBuiltInsFileName(
            fqName
        )

    fun getBuiltInsFileName(fqName: FqName): String =
        shortName(fqName) + "." + BUILTINS_FILE_EXTENSION

    private fun shortName(fqName: FqName): String =
        if (fqName.isRoot) "default-package" else fqName.shortName().asString()
}
