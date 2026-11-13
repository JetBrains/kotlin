/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.index

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.jvm.environment.JvmClasspathRootId
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassFileHandle
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.VirtualFileBackedClassFileHandle
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.VirtualFileBasedApi
import java.nio.file.Path

/** The [BinaryClassFileHandle] of the CLI: a file of a [JvmDependenciesIndex] together with the [JavaRoot] it was found in. */
class VfsClassFileHandle(private val file: VirtualFile, val root: JavaRoot) : VirtualFileBackedClassFileHandle {
    @VirtualFileBasedApi
    override val virtualFile: VirtualFile
        get() = file

    /** The content version of [file] at the time of the creation of this handle. */
    private val modificationStamp: Long = file.modificationStamp

    override val nameWithoutExtension: String
        get() = file.nameWithoutExtension

    override val extension: String
        get() = file.extension.orEmpty()

    override val rootId: JvmClasspathRootId
        get() = root.id

    override val rootPath: Path?
        get() = root.libraryPath()

    override val pathForDiagnostics: String
        get() = file.path

    override fun readBytes(): ByteArray = file.contentsToByteArray()

    override fun sibling(nameWithExtension: String): BinaryClassFileHandle? =
        file.parent?.findChild(nameWithExtension)?.let { VfsClassFileHandle(it, root) }

    override fun equals(other: Any?): Boolean =
        this === other ||
                other is VfsClassFileHandle &&
                file == other.file &&
                modificationStamp == other.modificationStamp

    override fun hashCode(): Int = file.hashCode()

    override fun toString(): String = file.toString()
}

fun IndexedClassFile.asBinaryClassFileHandle(): BinaryClassFileHandle = VfsClassFileHandle(file, root)
