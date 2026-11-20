/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.index.nio

import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.index.JavaRootRegistry
import org.jetbrains.kotlin.cli.jvm.index.JvmRootEntry
import org.jetbrains.kotlin.cli.jvm.index.JvmRootEntryFactory
import org.jetbrains.kotlin.cli.jvm.index.JvmRootLocation
import org.jetbrains.kotlin.cli.jvm.index.VfsRootEntry

/**
 * Creates NIO entries for the roots which have a NIO backend, and VFS entries for the others, by the [JvmRootLocation] of the root in
 * [roots]:
 *
 * - local directories: [PathRootEntry];
 * - jar archives (and directories in them): [JarRootEntry] of [jarPool];
 * - JDK modules of a `jrt:` file system: [PathRootEntry] over the module directory;
 * - `ct.sym` module roots of `-Xjdk-release`: [CtSymRootEntry] over [JarRootEntry]s of `ct.sym`.
 *
 * Without [jarPool] (mapped buffers cannot be unmapped on this JVM), archive and `ct.sym` roots get VFS entries.
 */
class NioRootEntryFactory(private val jarPool: JarFilePool?, private val roots: JavaRootRegistry) : JvmRootEntryFactory {
    override fun createRootEntry(root: JavaRoot): JvmRootEntry =
        nioRootEntry(roots.location(root.id)) ?: VfsRootEntry(roots.virtualFile(root))

    private fun nioRootEntry(location: JvmRootLocation): JvmRootEntry? = when (location) {
        is JvmRootLocation.LocalDirectory -> PathRootEntry.root(location.path)
        is JvmRootLocation.JrtModule -> PathRootEntry.root(location.path, location.pathForDiagnostics)
        is JvmRootLocation.ArchiveDirectory -> jarEntry(location)
        is JvmRootLocation.CtSym -> {
            val parts = location.parts.map { jarEntry(it) ?: return null }
            CtSymRootEntry.root(parts, location.packages, location.skipPackageCheck)
        }
        JvmRootLocation.VirtualOnly -> null
    }

    private fun jarEntry(location: JvmRootLocation.ArchiveDirectory): JarRootEntry? {
        val pool = jarPool ?: return null
        return pool.rootEntry(location.archive).findByRelativePath(location.pathInArchive)?.takeIf { it.isDirectory }
    }
}
