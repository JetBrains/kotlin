/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.index.nio

import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.index.JvmRootEntry
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassFileHandle
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path

/**
 * A [JvmRootEntry] over a [Path] of any file-system provider (the default one for directory roots, `jrt:` for JDK modules). It never
 * converts the path to a [java.io.File].
 *
 * A directory is listed once, on first use, into a map from the exact child name to the child entry, and the listing is kept for the
 * lifetime of the entry. [findChild] and [children] both use it, so names are matched case-sensitively even on case-insensitive hosts.
 * In line with the lifetime contract of the index, the listing is not refreshed and the entries are always [valid][isValid].
 *
 * The entries are reached under the lock of the index. The listing and the file type are still published safely, because the handles
 * created by [toHandle] look up their siblings in the parent's listing without the lock.
 */
class PathRootEntry private constructor(
    val path: Path,
    /** The slash-separated path from the root entry to this entry; empty for the root entry. */
    val relativePath: String,
    override val name: String,
    private val parent: PathRootEntry?,
    /** The diagnostic path of the root entry, if it is not the [path] itself (e.g. `<jdk home>!/modules/java.base` for JRT). */
    private val rootPathForDiagnostics: String?,
) : JvmRootEntry {
    override val nameWithoutExtension: String
        get() = name.substringBeforeLast('.')

    override val extension: String?
        get() = name.substringAfterLast('.', missingDelimiterValue = "").takeIf { name.contains('.') }

    override val isDirectory: Boolean by lazy(LazyThreadSafetyMode.PUBLICATION) { Files.isDirectory(path) }

    override val isValid: Boolean
        get() = true

    private val listing: Map<String, PathRootEntry> by lazy(LazyThreadSafetyMode.PUBLICATION) { list() }

    private fun list(): Map<String, PathRootEntry> {
        if (!isDirectory) return emptyMap()
        val result = LinkedHashMap<String, PathRootEntry>()
        try {
            Files.newDirectoryStream(path).use { stream ->
                // The order of the VFS: sorted by name on the local file system, as listed for `jrt:`.
                val children = if (path.fileSystem == FileSystems.getDefault()) stream.sortedBy { it.fileName?.toString() } else stream
                for (child in children) {
                    val childName = child.fileName?.toString()?.removeSuffix("/") ?: continue
                    val childRelativePath = if (relativePath.isEmpty()) childName else "$relativePath/$childName"
                    result[childName] = PathRootEntry(child, childRelativePath, childName, this, rootPathForDiagnostics)
                }
            }
        } catch (_: IOException) {
            // An unreadable directory has no children, as in the VFS.
        }
        return result
    }

    override fun findChild(name: String): JvmRootEntry? = listing[name]

    override val children: List<JvmRootEntry>
        get() = listing.values.toList()

    override fun containsExtensionRecursively(extension: String): Boolean =
        this.extension == extension || listing.values.any { it.containsExtensionRecursively(extension) }

    override fun toHandle(root: JavaRoot): BinaryClassFileHandle = PathClassFileHandle(this, root)

    internal fun sibling(name: String): PathRootEntry? = parent?.listing?.get(name)

    internal val pathForDiagnostics: String
        get() = when {
            rootPathForDiagnostics == null -> path.toString()
            relativePath.isEmpty() -> rootPathForDiagnostics
            else -> rootPathForDiagnostics.trimEnd('/') + "/" + relativePath
        }

    override fun equals(other: Any?): Boolean = this === other || other is PathRootEntry && path == other.path

    override fun hashCode(): Int = path.hashCode()

    override fun toString(): String = path.toString()

    companion object {
        /** The root entry of a root at [path]; diagnostics show [pathForDiagnostics] in place of [path], if given. */
        fun root(path: Path, pathForDiagnostics: String? = null): PathRootEntry =
            PathRootEntry(path, relativePath = "", name = path.fileName?.toString().orEmpty(), parent = null, pathForDiagnostics)
    }
}
