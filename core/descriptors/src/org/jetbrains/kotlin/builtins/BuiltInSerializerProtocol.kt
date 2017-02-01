/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.builtins

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol
import org.jetbrains.kotlin.serialization.builtins.BuiltInsProtoBuf

object BuiltInSerializerProtocol : SerializerExtensionProtocol(
        ExtensionRegistryLite.newInstance().apply { BuiltInsProtoBuf.registerAllExtensions(this) },
        BuiltInsProtoBuf.packageFqName,
        BuiltInsProtoBuf.constructorAnnotation, BuiltInsProtoBuf.classAnnotation, BuiltInsProtoBuf.functionAnnotation,
        BuiltInsProtoBuf.propertyAnnotation, BuiltInsProtoBuf.enumEntryAnnotation, BuiltInsProtoBuf.compileTimeValue,
        BuiltInsProtoBuf.parameterAnnotation, BuiltInsProtoBuf.typeAnnotation, BuiltInsProtoBuf.typeParameterAnnotation
) {
    val BUILTINS_FILE_EXTENSION = "kotlin_builtins"

    fun getBuiltInsFilePath(fqName: FqName): String =
            fqName.asString().replace('.', '/') + "/" + getBuiltInsFileName(fqName)

    fun getBuiltInsFileName(fqName: FqName): String =
            shortName(fqName) + "." + BUILTINS_FILE_EXTENSION

    private fun shortName(fqName: FqName): String =
            if (fqName.isRoot) "default-package" else fqName.shortName().asString()
}
