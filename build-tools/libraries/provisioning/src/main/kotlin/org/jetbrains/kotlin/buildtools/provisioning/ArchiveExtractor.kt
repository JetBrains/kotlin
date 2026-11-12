/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.provisioning

import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.ZipFile

/**
 * Unpacks an archive. The library implements it for zip ([Archives.zip]). Callers pass their own for other formats, for
 * example tar.
 *
 * An implementation must not write anything outside the destination, whatever the archive contains.
 */
public fun interface ArchiveExtractor {
    /** Unpacks [archive] into [destination], which exists and is a directory. */
    public fun extract(archive: Path, destination: Path)
}

/** The archive formats the library reads on its own. */
public object Archives {
    /**
     * Unpacks zip archives with the JDK. Entries that would land outside the destination fail with [UnsafeArchiveException].
     * Unix permissions and symlinks stored in the archive are not restored: the JDK does not expose them.
     */
    public fun zip(): ArchiveExtractor = ZipExtractor
}

private object ZipExtractor : ArchiveExtractor {
    override fun extract(archive: Path, destination: Path) {
        val base = destination.toAbsolutePath().normalize()
        val realBase = base.toRealPath()
        // directories already known to be inside the destination, so that each one is checked once
        val safeDirectories = hashSetOf(base)
        ZipFile(archive.toFile()).use { zip ->
            for (entry in zip.entries()) {
                val output = base.resolve(entry.name).normalize()
                if (output == base) continue
                if (!output.startsWith(base)) throw UnsafeArchiveException(archive, entry.name)

                val directory = if (entry.isDirectory) output else output.parent
                if (directory !in safeDirectories) {
                    // A symlink already in the destination must not lead the entry outside it. Check before creating
                    // anything: an empty directory in the wrong place is a write outside the destination too.
                    var existing = directory
                    while (!Files.exists(existing, LinkOption.NOFOLLOW_LINKS)) existing = existing.parent
                    if (!existing.toRealPath().startsWith(realBase)) throw UnsafeArchiveException(archive, entry.name)
                    Files.createDirectories(directory)
                    safeDirectories.add(directory)
                }
                if (!entry.isDirectory) {
                    zip.getInputStream(entry).use { input -> Files.copy(input, output, StandardCopyOption.REPLACE_EXISTING) }
                }
            }
        }
    }
}
