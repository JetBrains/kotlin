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

package org.jetbrains.kotlin.serialization

import com.google.protobuf.ExtensionRegistryLite
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

public abstract class SerializedResourcePaths {
    public abstract val extensionRegistry: ExtensionRegistryLite

    public abstract fun getClassMetadataPath(classId: ClassId): String

    public abstract fun getPackageFilePath(fqName: FqName): String

    public abstract fun getStringTableFilePath(fqName: FqName): String

    // TODO: remove this after M12
    public object FallbackPaths {
        public fun getPackageFilePath(fqName: FqName): String =
                fqName.asString().replace('.', '/') + "/.kotlin_package"

        public fun getStringTableFilePath(fqName: FqName): String =
                fqName.asString().replace('.', '/') + "/.kotlin_string_table"
    }

    public val fallbackPaths: FallbackPaths = FallbackPaths
}
