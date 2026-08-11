/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java.structure.impl.classFiles

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/**
 * The binary classpath of one compilation.
 */
interface BinaryClassFileIndex {
    /** The class files declaring [topLevelClassId], in classpath order. */
    fun findTopLevelClassFiles(topLevelClassId: ClassId): Collection<BinaryClassFileHandle>

    /**
     * The file names, without extension, of every class file directly in [packageFqName]. Names
     * containing `$` are returned as is; distinguishing a nested class from a top-level one with a
     * `$` in its name requires reading the class content and is left to the caller.
     */
    fun classFileNamesInPackage(packageFqName: FqName): Set<String>

    /** Whether any binary root contains the directory of [packageFqName]. */
    fun containsPackageDirectory(packageFqName: FqName): Boolean
}

fun interface BinaryClassFileScope {
    fun contains(classFile: BinaryClassFileHandle): Boolean
}

/**
 * One class file of a [BinaryClassFileIndex], and the key of everything read from it.
 *
 * Implementations must provide [equals] and [hashCode] identifying the file together with its content *version*.
 */
interface BinaryClassFileHandle {
    val nameWithoutExtension: String

    fun readBytes(): ByteArray
}
