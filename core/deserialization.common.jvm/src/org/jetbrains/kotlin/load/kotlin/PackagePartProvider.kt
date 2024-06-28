/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.serialization.deserialization.ClassData

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
     * This method is only for sake of optimization
     * @return package names set for which that provider has package parts
     */
    fun computePackageSetWithNonClassDeclarations(): Set<String>

    fun getAnnotationsOnBinaryModule(moduleName: String): List<ClassId>

    fun getAllOptionalAnnotationClasses(): List<ClassData>

    /**
     * Returns `true` if [getAllOptionalAnnotationClasses] may return a non-empty list.
     */
    fun mayHaveOptionalAnnotationClasses(): Boolean

    object Empty : PackagePartProvider {
        override fun findPackageParts(packageFqName: String): List<String> = emptyList()

        override fun getAnnotationsOnBinaryModule(moduleName: String): List<ClassId> = emptyList()

        override fun getAllOptionalAnnotationClasses(): List<ClassData> = emptyList()

        override fun mayHaveOptionalAnnotationClasses(): Boolean = false

        override fun computePackageSetWithNonClassDeclarations(): Set<String> = emptySet()
    }
}
