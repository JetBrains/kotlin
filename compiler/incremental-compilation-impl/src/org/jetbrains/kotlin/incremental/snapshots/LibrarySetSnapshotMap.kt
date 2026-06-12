/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.snapshots

import org.jetbrains.kotlin.incremental.IncrementalCompilationContext
import org.jetbrains.kotlin.incremental.IncrementalModuleInfo
import org.jetbrains.kotlin.incremental.dirtyFiles.expandClasspathFiles
import org.jetbrains.kotlin.incremental.storage.AbstractBasicMap
import org.jetbrains.kotlin.incremental.storage.StringExternalizer
import org.jetbrains.kotlin.incremental.storage.toDescriptor
import java.io.File

/**
 * Tracks the set of library files on the classpath at a coarse, path-independent granularity.
 *
 * Keys are stable across project relocation and user-home swaps:
 *  - Local libraries (resolved via [IncrementalModuleInfo.jarToModule] or [IncrementalModuleInfo.dirToModule])
 *    are identified by their module name, so a rebuilt JAR/KLIB of the same module shows up as a content
 *    change under the same key — and routes through the history-file diff in
 *    [org.jetbrains.kotlin.incremental.dirtyFiles.getClasspathChanges].
 *  - External libraries (no module entry, no history file) are identified by their content hash, so any change
 *    manifests as an "old key disappears, new key appears" pair, tripping the existing
 *    "no history file → full rebuild" path.
 * This is to avoid dealing with the problem of relocatability based on paths.
 *
 * Note: distinct from the unrelated [org.jetbrains.kotlin.incremental.classpathDiff] machinery
 * ("classpath snapshot" in that area refers to ABI-level snapshots, not the library-set tracking done here).
 * This is purely required for the history files-based approach.
 */
internal class LibrarySetSnapshotMap(
    storageFile: File,
    icContext: IncrementalCompilationContext,
) : AbstractBasicMap<String, FileSnapshot>(
    storageFile,
    StringExternalizer.toDescriptor(),
    FileSnapshotExternalizer,
    icContext
) {
    @Synchronized
    fun compareAndUpdate(
        classpath: List<File>,
        modulesInfo: IncrementalModuleInfo?,
    ): LibrarySetChanges {
        val provider = SimpleFileSnapshotProviderImpl()
        val seenKeys = HashSet<String>()
        val modifiedFiles = HashSet<File>()
        val addedKeys = HashSet<String>()

        for (file in expandClasspathFiles(classpath)) {
            if (!file.isFile) continue
            // if it's a non-packed KLIB directory, we are going 2 levels up from default/manifest to the klib target directory
            val moduleEntry = modulesInfo?.jarToModule?.get(file)
                ?: modulesInfo?.dirToModule?.get(file.parentFile.parentFile)
            val snapshot = provider[file]
            val key = if (moduleEntry != null) "local:${moduleEntry.name}"
            else "ext:${snapshot.hash.toHexString()}"
            seenKeys += key

            val previous = this[key]
            if (previous == null) {
                // Never-seen key: the project has not compiled against this library before, so any incremental delta
                // from the library's own history would be partial — we have to rebuild instead.
                addedKeys += key
                this[key] = snapshot
            } else if (previous != snapshot) {
                modifiedFiles += file
                this[key] = snapshot
            }
        }

        val removedKeys = keys.toSet() - seenKeys
        removedKeys.forEach { remove(it) }
        return LibrarySetChanges(modifiedFiles, addedKeys, removedKeys)
    }
}

internal data class LibrarySetChanges(
    val modifiedFiles: Set<File>,
    val addedKeys: Set<String>,
    val removedKeys: Set<String>,
)

/**
 * Prefix used to mark a [File] that is not a real path but a sentinel carrying the identity of a library
 * (local module or external content hash) that was present in the previous build and is now gone.
 *
 * Downstream code paths that consume `ChangedFiles.removed` must recognize this prefix to treat such entries
 * as library removals; see usages in `getClasspathChanges`.
 */
internal const val LIBRARY_SET_REMOVED_SENTINEL_PREFIX = "library-set-removed:"

internal fun librarySetRemovedSentinel(key: String): File = File(LIBRARY_SET_REMOVED_SENTINEL_PREFIX + key)

internal fun File.isLibrarySetRemovedSentinel(): Boolean = path.startsWith(LIBRARY_SET_REMOVED_SENTINEL_PREFIX)

/**
 * Symmetric to [LIBRARY_SET_REMOVED_SENTINEL_PREFIX]: marks a [File] that is not a real path but a sentinel for a
 * library that was NOT on the classpath in the previous build but is on it now. The library's own build history
 * (if any) only contains deltas since some past build, not the cumulative state — so an incremental delta would
 * miss every symbol the library exposed before its first observed diff. Downstream code paths must recognize this
 * prefix and treat such entries as a rebuild trigger; see usages in `getClasspathChanges`.
 */
internal const val LIBRARY_SET_ADDED_SENTINEL_PREFIX = "library-set-added:"

internal fun librarySetAddedSentinel(key: String): File = File(LIBRARY_SET_ADDED_SENTINEL_PREFIX + key)

internal fun File.isLibrarySetAddedSentinel(): Boolean = path.startsWith(LIBRARY_SET_ADDED_SENTINEL_PREFIX)

private fun ByteArray.toHexString(): String =
    joinToString(separator = "") { "%02x".format(it) }
