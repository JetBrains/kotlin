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

import com.google.protobuf.ExtensionRegistryLite
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.builtins.BuiltInsProtoBuf

public object BuiltInsSerializationUtil {
    public val EXTENSION_REGISTRY: ExtensionRegistryLite

    init {
        EXTENSION_REGISTRY = ExtensionRegistryLite.newInstance()
        BuiltInsProtoBuf.registerAllExtensions(EXTENSION_REGISTRY)
    }

    private val CLASS_METADATA_FILE_EXTENSION = "kotlin_class"
    private val PACKAGE_FILE_EXTENSION = "kotlin_package"
    private val STRING_TABLE_FILE_EXTENSION = "kotlin_string_table"

    // TODO: remove this after M12
    public object FallbackPaths {
        public fun getPackageFilePath(fqName: FqName): String =
                packageFqNameToPath(fqName) + "/.kotlin_package"

        public fun getStringTableFilePath(fqName: FqName): String =
                packageFqNameToPath(fqName) + "/.kotlin_string_table"
    }

    public fun getClassMetadataPath(classId: ClassId): String {
        return packageFqNameToPath(classId.getPackageFqName()) + "/" + classId.getRelativeClassName().asString() +
               "." + CLASS_METADATA_FILE_EXTENSION
    }

    public fun getPackageFilePath(fqName: FqName): String =
            packageFqNameToPath(fqName) + "/" + shortName(fqName) + "." + PACKAGE_FILE_EXTENSION

    public fun getStringTableFilePath(fqName: FqName): String =
            packageFqNameToPath(fqName) + "/" + shortName(fqName) + "." + STRING_TABLE_FILE_EXTENSION


    private fun packageFqNameToPath(fqName: FqName): String =
            fqName.asString().replace('.', '/')

    private fun shortName(fqName: FqName): String =
            if (fqName.isRoot()) "default-package" else fqName.shortName().asString()
}
