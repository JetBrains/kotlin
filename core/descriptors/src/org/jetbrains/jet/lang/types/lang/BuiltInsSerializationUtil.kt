/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.types.lang

import org.jetbrains.jet.lang.resolve.name.*
import kotlin.platform.platformStatic

public object BuiltInsSerializationUtil {
    private val CLASS_METADATA_FILE_EXTENSION = "kotlin_class"
    private val PACKAGE_FILE_NAME = ".kotlin_package"
    private val STRING_TABLE_FILE_NAME = ".kotlin_string_table"
    private val CLASS_NAMES_FILE_NAME = ".kotlin_class_names"
    private val CLASS_OBJECT_NAME = "object"

    private fun relativeClassNameToFilePath(className: FqNameUnsafe): String? {
        return FqName.fromSegments(className.pathSegments().map { segment ->
            when {
                SpecialNames.isClassObjectName(segment) -> CLASS_OBJECT_NAME
                !segment.isSpecial() -> segment.getIdentifier()
                else -> return null
            }
        }).asString()
    }

    platformStatic public fun getClassMetadataPath(classId: ClassId): String? {
        val filePath = relativeClassNameToFilePath(classId.getRelativeClassName())
        if (filePath == null) return null
        return packageFqNameToPath(classId.getPackageFqName()) + "/" + filePath + "." + CLASS_METADATA_FILE_EXTENSION
    }

    platformStatic public fun getPackageFilePath(fqName: FqName): String =
            packageFqNameToPath(fqName) + "/" + PACKAGE_FILE_NAME

    platformStatic public fun getStringTableFilePath(fqName: FqName): String =
            packageFqNameToPath(fqName) + "/" + STRING_TABLE_FILE_NAME

    platformStatic public fun getClassNamesFilePath(fqName: FqName): String =
            packageFqNameToPath(fqName) + "/" + CLASS_NAMES_FILE_NAME

    private fun packageFqNameToPath(fqName: FqName): String =
            fqName.asString().replace('.', '/')
}
