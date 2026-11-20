/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.index

import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.io.URLUtil.JAR_SEPARATOR
import org.jetbrains.kotlin.cli.jvm.modules.CoreJrtVirtualFile
import org.jetbrains.kotlin.cli.jvm.modules.CtSymDirectoryContainer
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths

/** Where a classpath root lies, as far as a NIO entry can read it (see [JavaRootRegistry.location]). */
sealed class JvmRootLocation {
    /** A directory of the local file system. */
    data class LocalDirectory(val path: Path) : JvmRootLocation()

    /** The directory [pathInArchive] (empty for the archive root) of the local archive [archive]. */
    data class ArchiveDirectory(val archive: Path, val pathInArchive: String) : JvmRootLocation()

    /** A JDK module directory of a `jrt:` file system, reported as [pathForDiagnostics]. */
    data class JrtModule(val path: Path, val pathForDiagnostics: String) : JvmRootLocation()

    /** A `ct.sym` module root of `-Xjdk-release`: the union of [parts], restricted to [packages] unless [skipPackageCheck]. */
    data class CtSym(
        val parts: List<ArchiveDirectory>,
        val packages: Map<String, Boolean>,
        val skipPackageCheck: Boolean,
    ) : JvmRootLocation()

    /** A root that only its [VirtualFile] can read, e.g. an in-memory root of a build system. */
    data object VirtualOnly : JvmRootLocation()

    companion object {
        fun of(file: VirtualFile): JvmRootLocation = when {
            file is CtSymDirectoryContainer -> ctSym(file)
            file is CoreJrtVirtualFile -> JrtModule(file.nioPath, file.path)
            file.fileSystem.protocol == StandardFileSystems.FILE_PROTOCOL && file.isDirectory ->
                localPath(file.path)?.let(::LocalDirectory)
            file.fileSystem.protocol == StandardFileSystems.JAR_PROTOCOL -> archiveDirectory(file.path)
            else -> null
        } ?: VirtualOnly

        /** The directory at a VFS archive path `<archive>!/<path in archive>`. */
        private fun archiveDirectory(path: String): ArchiveDirectory? {
            val separator = path.indexOf(JAR_SEPARATOR).takeIf { it >= 0 } ?: return null
            val archive = localPath(path.substring(0, separator)) ?: return null
            return ArchiveDirectory(archive, path.substring(separator + JAR_SEPARATOR.length))
        }

        private fun ctSym(container: CtSymDirectoryContainer): CtSym? {
            if (container.currentPackage.isNotEmpty()) return null
            val parts = container.rootOrPackageParts.map { archiveDirectory(it.path) ?: return null }
            return CtSym(parts, container.packages, container.skipPackageCheck)
        }

        private fun localPath(path: String): Path? =
            try {
                Paths.get(path)
            } catch (_: InvalidPathException) {
                null
            }
    }
}
