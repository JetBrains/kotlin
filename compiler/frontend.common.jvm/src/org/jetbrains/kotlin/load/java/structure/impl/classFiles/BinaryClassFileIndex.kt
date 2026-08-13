/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java.structure.impl.classFiles

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import java.nio.file.Path

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

/**
 * One class file of a [BinaryClassFileIndex], and the key of everything read from it.
 *
 * Implementations must provide [equals] and [hashCode] identifying the file together with its content *version*.
 */
interface BinaryClassFileHandle {
    val nameWithoutExtension: String

    /**
     * Whether this class file lies in [classpathRoot] — a directory or a `.jar`/`.jmod` file, spelled as it is on the
     * compiler's classpath and resolved against the working directory if relative. This is all a lookup needs in
     * order to be restricted to a part of the classpath, an index being the classpath of the whole compilation; how
     * a class file is located stays with the implementation.
     */
    fun isUnder(classpathRoot: Path): Boolean

    fun readBytes(): ByteArray
}
