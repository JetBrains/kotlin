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
import org.jetbrains.kotlin.serialization.SerializedResourcePaths
import org.jetbrains.kotlin.serialization.builtins.BuiltInsProtoBuf

public object BuiltInsSerializedResourcePaths : SerializedResourcePaths() {
    public override val extensionRegistry: ExtensionRegistryLite

    init {
        extensionRegistry = ExtensionRegistryLite.newInstance()
        BuiltInsProtoBuf.registerAllExtensions(extensionRegistry)
    }

    val CLASS_METADATA_FILE_EXTENSION = "kotlin_class"
    val PACKAGE_FILE_EXTENSION = "kotlin_package"
    val STRING_TABLE_FILE_EXTENSION = "kotlin_string_table"

    public override fun getClassMetadataPath(classId: ClassId): String {
        return packageFqNameToPath(classId.getPackageFqName()) + "/" + classId.getRelativeClassName().asString() +
               "." + CLASS_METADATA_FILE_EXTENSION
    }

    public override fun getPackageFilePath(fqName: FqName): String =
            packageFqNameToPath(fqName) + "/" + shortName(fqName) + "." + PACKAGE_FILE_EXTENSION

    public override fun getStringTableFilePath(fqName: FqName): String =
            packageFqNameToPath(fqName) + "/" + shortName(fqName) + "." + STRING_TABLE_FILE_EXTENSION


    private fun packageFqNameToPath(fqName: FqName): String =
            fqName.asString().replace('.', '/')

    private fun shortName(fqName: FqName): String =
            if (fqName.isRoot()) "default-package" else fqName.shortName().asString()
}
