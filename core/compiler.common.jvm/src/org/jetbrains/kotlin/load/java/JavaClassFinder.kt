/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java

import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaPackage
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

interface JavaClassFinder {
    data class Request(
        val classId: ClassId,
        @Suppress("ArrayInDataClass")
        val previouslyFoundClassFileContent: ByteArray? = null,
        val outerClass: JavaClass? = null
    )

    fun findClass(request: Request): JavaClass?
    fun findClass(classId: ClassId): JavaClass? = findClass(Request(classId))

    /**
     * Finds all classes with the specified [ClassId]. This function should be used if the search space permits such ambiguities and if
     * [findClass] is not guaranteed to disambiguate by itself. For example, in an IDE context, a broad search scope might lead to multiple
     * valid candidates, which need to be disambiguated according to classpath order.
     *
     * [findClasses] may return a single [JavaClass], even if more could be found, if the resulting [JavaClass] is guaranteed to be the
     * first in the dependency order.
     */
    fun findClasses(request: Request): List<JavaClass>

    fun findClasses(classId: ClassId): List<JavaClass> = findClasses(Request(classId))

    fun findPackage(fqName: FqName, mayHaveAnnotations: Boolean = true): JavaPackage?

    fun knownClassNamesInPackage(packageFqName: FqName): Set<String>?

    /**
     * Whether [knownClassNamesInPackage] can be computed. When [canComputeKnownClassNamesInPackage] is `false`, [knownClassNamesInPackage]
     * will always return `null`.
     */
    fun canComputeKnownClassNamesInPackage(): Boolean
}
