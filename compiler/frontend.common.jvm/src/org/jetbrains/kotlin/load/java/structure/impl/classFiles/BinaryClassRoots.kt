/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java.structure.impl.classFiles

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/**
 * The binary classpath of one compilation as seen by a single session, exposing only what a binary
 * `JavaClassFinder` asks of it. The session's search scope is applied by the implementation, so
 * consumers need no scope object of their own.
 */
interface BinaryClassRoots {
    /**
     * The class files declaring [topLevelClassId], which must be a top-level class id.
     */
    fun findTopLevelClassFiles(topLevelClassId: ClassId): TopLevelClassFileCandidates

    /**
     * The file names, without extension, of every class file directly in [packageFqName]. Names
     * containing `$` are returned as they are; telling a nested class from a top-level one with a
     * `$` in its name requires reading the class content and is left to the caller.
     */
    fun classFileNamesInPackage(packageFqName: FqName): Set<String>

    /** Whether any binary root contains the directory of [packageFqName]. */
    fun containsPackageDirectory(packageFqName: FqName): Boolean
}

/**
 * The two answers the classpath can give for one top-level class name: the classpath-order winner
 * ([anywhere], for cross-references out of bytecode, which resolve against the whole classpath) and
 * the first candidate that is also in the session's scope ([inScope], for the session's own lookups).
 */
class TopLevelClassFileCandidates(val anywhere: BinaryClassFileHandle?, val inScope: BinaryClassFileHandle?)

/**
 * A single binary class file in a classpath root.
 *
 * [virtualFile] is the transitional accessor for [BinaryJavaClass], which is still
 * `VirtualFile`-bound; nothing outside `frontend.common.jvm` is meant to read it.
 */
interface BinaryClassFileHandle {
    val nameWithoutExtension: String

    fun readBytes(): ByteArray

    val virtualFile: VirtualFile
}

fun VirtualFile.asBinaryClassFileHandle(): BinaryClassFileHandle = VirtualFileBinaryClassFileHandle(this)

private class VirtualFileBinaryClassFileHandle(override val virtualFile: VirtualFile) : BinaryClassFileHandle {
    override val nameWithoutExtension: String
        get() = virtualFile.nameWithoutExtension

    override fun readBytes(): ByteArray = virtualFile.contentsToByteArray()

    override fun toString(): String = virtualFile.toString()
}
