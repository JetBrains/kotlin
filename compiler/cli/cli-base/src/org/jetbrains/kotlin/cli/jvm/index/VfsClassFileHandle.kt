/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.index

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.jvm.environment.JvmClasspathRootId
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassFileHandle
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.VirtualFileBackedClassFileHandle
import java.nio.file.Path

/** The [BinaryClassFileHandle] of the CLI: a file of a [JvmDependenciesIndex] together with the [JavaRoot] it was found in. */
class VfsClassFileHandle(override val virtualFile: VirtualFile, val root: JavaRoot) : VirtualFileBackedClassFileHandle {
    /** The content version of [virtualFile] at the time of the creation of this handle. */
    private val modificationStamp: Long = virtualFile.modificationStamp

    override val nameWithoutExtension: String
        get() = virtualFile.nameWithoutExtension

    override val extension: String
        get() = virtualFile.extension.orEmpty()

    override val rootId: JvmClasspathRootId
        get() = root.id

    override val rootPath: Path?
        get() = root.libraryPath

    override val pathForDiagnostics: String
        get() = virtualFile.path

    override fun readBytes(): ByteArray = virtualFile.contentsToByteArray()

    override fun sibling(nameWithExtension: String): BinaryClassFileHandle? =
        virtualFile.parent?.findChild(nameWithExtension)?.let { VfsClassFileHandle(it, root) }

    override fun equals(other: Any?): Boolean =
        this === other ||
                other is VfsClassFileHandle &&
                virtualFile == other.virtualFile &&
                modificationStamp == other.modificationStamp

    override fun hashCode(): Int = virtualFile.hashCode()

    override fun toString(): String = virtualFile.toString()
}

fun IndexedClassFile.asBinaryClassFileHandle(): BinaryClassFileHandle = VfsClassFileHandle(file, root)
