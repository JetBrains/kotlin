/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java.structure.impl.classFiles

import org.jetbrains.kotlin.jvm.environment.JvmClasspath
import org.jetbrains.kotlin.jvm.environment.JvmClasspathRootId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import java.nio.file.Path

/**
 * The binary classpath of one compilation.
 */
interface BinaryClassFileIndex {
    /**
     * Changes whenever roots are added to the classpath behind the index. A compilation may extend its own classpath
     * while running — scripting resolves `@DependsOn` against the sources it is compiling — and everything looked up
     * before that point may be missing the new roots, so a lookup cache over the index must be dropped when this changes.
     */
    val classpathVersion: Int get() = 0

    /** Returns class files declaring [topLevelClassId], in classpath order. */
    fun findTopLevelClassFiles(topLevelClassId: ClassId): Collection<BinaryClassFileHandle>

    /**
     * The file names, without extension, of every class file directly in [packageFqName]. Names
     * containing `$` are returned as is; distinguishing a nested class from a top-level one with a
     * `$` in its name requires reading the class content and is left to the caller.
     */
    fun classFileNamesInPackage(packageFqName: FqName): Set<String>

    fun containsPackageDirectory(packageFqName: FqName): Boolean
}

/**
 * One class file of a [BinaryClassFileIndex], and the key of everything read from it.
 *
 * Implementations must provide [equals] and [hashCode] identifying the file together with its content *version*.
 */
interface BinaryClassFileHandle {
    val nameWithoutExtension: String

    /** `class`, `sig`, ... */
    val extension: String

    /** The classpath root this file was found in, as the index assigned it: not inferred from the file's path. */
    val rootId: JvmClasspathRootId

    /**
     * The library [rootId] belongs to, as a local path: the `.jar` file, the directory, or the JDK home for JDK roots.
     * `null` for roots which have no local path (e.g. the in-memory output of a build system).
     */
    val rootPath: Path?

    /** The path rendered in diagnostics and messages, e.g. `/lib/a.jar!/p/A.class`. */
    val pathForDiagnostics: String

    fun isUnder(classpathRoot: JvmClasspathRootId): Boolean = rootId == classpathRoot

    fun readBytes(): ByteArray

    /** The file named [nameWithExtension] in the same directory and the same root, if it exists. */
    fun sibling(nameWithExtension: String): BinaryClassFileHandle?
}

operator fun JvmClasspath.contains(classFile: BinaryClassFileHandle): Boolean = when (this) {
    is JvmClasspath.Roots -> roots.any(classFile::isUnder)
    is JvmClasspath.ProjectLibraries -> excludedRoots.none(classFile::isUnder)
}
