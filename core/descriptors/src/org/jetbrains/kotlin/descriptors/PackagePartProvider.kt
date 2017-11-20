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

package org.jetbrains.kotlin.descriptors

interface PackagePartProvider {
    /**
     * @return JVM internal names of package parts existing in the package with the given FQ name.
     *
     * For example, if a file named foo.kt in package org.test is compiled to a library, PackagePartProvider for such library
     * must return the list `["org/test/FooKt"]` for the query `"org.test"`
     * (in case the file is not annotated with @JvmName, @JvmPackageName or @JvmMultifileClass).
     */
    fun findPackageParts(packageFqName: String): List<String>

    /**
     * @return simple names of .kotlin_metadata files that store data for top level declarations in the package with the given FQ name
     */
    fun findMetadataPackageParts(packageFqName: String): List<String>

    object Empty : PackagePartProvider {
        override fun findPackageParts(packageFqName: String): List<String> = emptyList()

        override fun findMetadataPackageParts(packageFqName: String): List<String> = emptyList()
    }
}
