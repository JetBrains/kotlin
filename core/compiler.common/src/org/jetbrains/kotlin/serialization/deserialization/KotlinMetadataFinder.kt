/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.serialization.deserialization

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import java.io.InputStream

interface KotlinMetadataFinder {
    /**
     * @return an [InputStream] which should be used to load the .kotlin_metadata file for class with the given [classId].
     * [classId] identifies either a real top level class, or a package part (e.g. it can be "foo/bar/_1Kt")
     */
    fun findMetadata(classId: ClassId): InputStream?

    fun findMetadataTopLevelClassesInPackage(packageFqName: FqName): Set<String>?

    /**
     * @return `true` iff this finder is able to locate the package with the given [fqName], containing .kotlin_metadata files.
     * Note that returning `true` makes [MetadataPackageFragmentProvider] construct the package fragment for the package,
     * and that fact can alter the qualified name expression resolution in the compiler front-end
     */
    fun hasMetadataPackage(fqName: FqName): Boolean

    /**
     * @return an [InputStream] which should be used to load the .kotlin_builtins file for package with the given [packageFqName].
     */
    fun findBuiltInsData(packageFqName: FqName): InputStream?
}
