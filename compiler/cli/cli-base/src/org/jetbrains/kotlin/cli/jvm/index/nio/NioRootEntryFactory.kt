/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.index.nio

import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.io.URLUtil.JAR_SEPARATOR
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.index.JvmRootEntry
import org.jetbrains.kotlin.cli.jvm.index.JvmRootEntryFactory
import org.jetbrains.kotlin.cli.jvm.index.VfsRootEntry
import org.jetbrains.kotlin.cli.jvm.modules.CoreJrtVirtualFile
import org.jetbrains.kotlin.cli.jvm.modules.CtSymDirectoryContainer
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Creates NIO entries for the roots which have a NIO backend, and VFS entries for the others.
 *
 * - local directories: [PathRootEntry];
 * - jar archives (and directories in them): [JarRootEntry] of [jarPool];
 * - JDK modules of a `jrt:` file system: [PathRootEntry] over the module directory;
 * - `ct.sym` module roots of `-Xjdk-release`: [CtSymRootEntry] over [JarRootEntry]s of `ct.sym`.
 *
 * Without [jarPool] (mapped buffers cannot be unmapped on this JVM), archive and `ct.sym` roots get VFS entries.
 * The root is still described by its [JavaRoot.file]; the factory only reads where the root lies.
 */
class NioRootEntryFactory(private val jarPool: JarFilePool?) : JvmRootEntryFactory {
    override fun createRootEntry(root: JavaRoot): JvmRootEntry = nioRootEntry(root.file) ?: VfsRootEntry(root.file)

    private fun nioRootEntry(file: VirtualFile): JvmRootEntry? = when {
        file is CtSymDirectoryContainer -> ctSymRootEntry(file)
        file is CoreJrtVirtualFile -> PathRootEntry.root(file.nioPath, file.path)
        file.fileSystem.protocol == StandardFileSystems.FILE_PROTOCOL && file.isDirectory ->
            localPath(file.path)?.let(PathRootEntry::root)
        file.fileSystem.protocol == StandardFileSystems.JAR_PROTOCOL -> jarEntry(file.path)
        else -> null
    }

    /** The entry at a VFS archive path `<archive>!/<path in archive>`. */
    private fun jarEntry(path: String): JarRootEntry? {
        val pool = jarPool ?: return null
        val separator = path.indexOf(JAR_SEPARATOR).takeIf { it >= 0 } ?: return null
        val archive = localPath(path.substring(0, separator)) ?: return null
        val pathInArchive = path.substring(separator + JAR_SEPARATOR.length)
        return pool.rootEntry(archive).findByRelativePath(pathInArchive)?.takeIf { it.isDirectory }
    }

    private fun ctSymRootEntry(container: CtSymDirectoryContainer): JvmRootEntry? {
        if (container.currentPackage.isNotEmpty()) return null
        val parts = container.rootOrPackageParts.map { jarEntry(it.path) ?: return null }
        return CtSymRootEntry.root(parts, container.packages, container.skipPackageCheck)
    }

    private fun localPath(path: String): Path? =
        try {
            Paths.get(path)
        } catch (_: InvalidPathException) {
            null
        }
}
