/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.index.nio

import com.intellij.openapi.vfs.StandardFileSystems
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.index.JvmRootEntry
import org.jetbrains.kotlin.cli.jvm.index.JvmRootEntryFactory
import org.jetbrains.kotlin.cli.jvm.index.VfsRootEntry
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Creates NIO entries for the roots which have a NIO backend, and VFS entries for the others.
 *
 * NIO entries exist for local directory roots so far; archive, JRT and `ct.sym` roots get VFS entries.
 */
object NioRootEntryFactory : JvmRootEntryFactory {
    override fun createRootEntry(root: JavaRoot): JvmRootEntry =
        localDirectoryPath(root)?.let(PathRootEntry::root) ?: VfsRootEntry(root.file)

    private fun localDirectoryPath(root: JavaRoot): Path? {
        val file = root.file
        if (file.fileSystem.protocol != StandardFileSystems.FILE_PROTOCOL || !file.isDirectory) return null
        return try {
            Paths.get(file.path)
        } catch (_: InvalidPathException) {
            null
        }
    }
}
