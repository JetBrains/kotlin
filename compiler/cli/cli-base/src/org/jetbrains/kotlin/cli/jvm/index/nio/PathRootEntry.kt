/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.index.nio

import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.index.JvmRootEntry
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassFileHandle
import java.io.IOException
import java.nio.file.DirectoryIteratorException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

/**
 * A [JvmRootEntry] over a [Path] of a local directory root.
 *
 * A directory is listed once, on first use, into a map from the exact child name to the child entry, which serves [findChild] and
 * [children]: names are matched case-sensitively even on case-insensitive hosts. The listing is never refreshed, and the entries are
 * always [valid][isValid]: a file deleted after listing fails on read.
 *
 * The listing and the file type are published safely: the handles look up their siblings in the parent's listing without the index lock.
 */
class PathRootEntry private constructor(
    val path: Path,
    /** The slash-separated path from the root entry to this entry; empty for the root entry. */
    val relativePath: String,
    override val name: String,
    private val parent: PathRootEntry?,
) : JvmRootEntry {
    /** `null` until the file type is read, then [FILE], [UNLISTED_DIRECTORY] or, once listed, the [Listing] of the directory. */
    @Volatile
    private var state: Any? = null

    override val nameWithoutExtension: String
        get() = name.withoutExtension()

    override val extension: String?
        get() = name.extensionOrNull()

    override val isDirectory: Boolean
        get() {
            state?.let { return it !== FILE }
            val isDirectory = Files.isDirectory(path)
            STATE.compareAndSet(this, null, if (isDirectory) UNLISTED_DIRECTORY else FILE)
            return isDirectory
        }

    override val isValid: Boolean
        get() = true

    private val listing: Listing
        get() {
            (state as? Listing)?.let { return it }
            if (!isDirectory) return Listing.EMPTY
            val listing = list()
            return if (STATE.compareAndSet(this, UNLISTED_DIRECTORY, listing)) listing else state as Listing
        }

    private fun list(): Listing {
        val result = LinkedHashMap<String, PathRootEntry>()
        try {
            Files.newDirectoryStream(path).use { stream ->
                // Unsorted, as in the VFS.
                for (child in stream) {
                    val childName = child.fileName?.toString() ?: continue
                    val childRelativePath = if (relativePath.isEmpty()) childName else "$relativePath/$childName"
                    result[childName] = PathRootEntry(child, childRelativePath, childName, this)
                }
            }
        } catch (_: IOException) {
            // An unreadable directory has no children, as in the VFS.
            result.clear()
        } catch (_: DirectoryIteratorException) {
            // The same, for an I/O error during the iteration.
            result.clear()
        }
        return Listing(result)
    }

    override fun findChild(name: String): JvmRootEntry? = listing.byName[name]

    override val children: List<JvmRootEntry>
        get() = listing.entries

    override fun containsExtensionRecursively(extension: String): Boolean =
        this.extension == extension || listing.entries.any { it.containsExtensionRecursively(extension) }

    override fun toHandle(root: JavaRoot): BinaryClassFileHandle = PathClassFileHandle(this, root)

    internal fun sibling(name: String): PathRootEntry? = parent?.listing?.byName?.get(name)

    override fun toString(): String = path.toString()

    private class Listing(val byName: Map<String, PathRootEntry>) {
        val entries: List<JvmRootEntry> = byName.values.toList()

        companion object {
            val EMPTY = Listing(emptyMap())
        }
    }

    companion object {
        private val FILE = Any()
        private val UNLISTED_DIRECTORY = Any()

        private val STATE: AtomicReferenceFieldUpdater<PathRootEntry, Any> =
            AtomicReferenceFieldUpdater.newUpdater(PathRootEntry::class.java, Any::class.java, "state")

        /** The root entry of a root at [path]. */
        fun root(path: Path): PathRootEntry =
            PathRootEntry(path, relativePath = "", name = path.fileName?.toString().orEmpty(), parent = null)
    }
}

/** The extension of a file name without the dot, or `null` if the name has none. */
internal fun String.extensionOrNull(): String? = if (contains('.')) substringAfterLast('.') else null

internal fun String.withoutExtension(): String = substringBeforeLast('.')
