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

/** The [BinaryClassFileHandle] of the VFS-based [JvmDependenciesIndex]es: a file together with the [JavaRoot] it was found in. */
class VfsClassFileHandle(private val file: VirtualFile, val root: JavaRoot) : VirtualFileBackedClassFileHandle {
    @VirtualFileBasedApi
    override val virtualFile: VirtualFile
        get() = file

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
                root == other.root

    override fun hashCode(): Int = file.hashCode()

    override fun toString(): String = file.toString()
}

/**
 * The [VirtualFile] of a file found in a VFS-based [JvmDependenciesIndex]. Only for code which still works on [VirtualFile]s:
 * the PSI Java view, the Analysis API and the backend's inline lookup.
 */
@VirtualFileBasedApi
val BinaryClassFileHandle.virtualFile: VirtualFile
    get() = (this as VirtualFileBackedClassFileHandle).virtualFile
