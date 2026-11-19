/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.index

import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassFileHandle

/** The [JvmRootEntry] over a [VirtualFile]. */
class VfsRootEntry(val file: VirtualFile) : JvmRootEntry {
    override val name: String
        get() = file.name

    override val nameWithoutExtension: String
        get() = file.nameWithoutExtension

    override val extension: String?
        get() = file.extension

    override val isDirectory: Boolean
        get() = file.isDirectory

    override val isValid: Boolean
        get() = file.isValid

    override fun findChild(name: String): JvmRootEntry? = file.findChild(name)?.let(::VfsRootEntry)

    override val children: List<JvmRootEntry>
        get() {
            val files = file.children
            return object : AbstractList<JvmRootEntry>() {
                override val size: Int get() = files.size
                override fun get(index: Int): JvmRootEntry = VfsRootEntry(files[index])
            }
        }

    override fun containsExtensionRecursively(extension: String): Boolean =
        !VfsUtilCore.processFilesRecursively(file) { it.extension != extension }

    override fun toHandle(root: JavaRoot): BinaryClassFileHandle = VfsClassFileHandle(file, root)

    override fun toString(): String = file.toString()
}
