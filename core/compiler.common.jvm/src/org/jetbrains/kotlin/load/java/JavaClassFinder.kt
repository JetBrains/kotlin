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

    fun findPackage(fqName: FqName): JavaPackage?

    fun knownClassNamesInPackage(packageFqName: FqName): Set<String>?
}
