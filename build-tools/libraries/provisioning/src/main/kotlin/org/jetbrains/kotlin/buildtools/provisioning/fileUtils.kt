/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.provisioning

import java.io.IOException
import java.nio.file.AccessDeniedException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.DosFileAttributeView
import java.nio.file.attribute.PosixFileAttributes
import java.nio.file.attribute.PosixFilePermission

private val OWNER_ACCESS = setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE)

/**
 * Deletes [path] and everything below it. Symlinks and Windows junctions are deleted, never followed, so nothing outside
 * [path] is touched. Read-only files and directories are deleted too. A missing [path] is not an error.
 */
internal fun deleteRecursively(path: Path) {
    if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return
    Files.walkFileTree(path, object : SimpleFileVisitor<Path>() {
        override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
            // A Windows junction looks like a directory, but what is behind it is not ours. Remove the junction itself.
            if (attrs.isOther) {
                delete(dir)
                return FileVisitResult.SKIP_SUBTREE
            }
            makeAccessible(dir)
            return FileVisitResult.CONTINUE
        }

        override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
            delete(file)
            return FileVisitResult.CONTINUE
        }

        override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult {
            when {
                exc is NoSuchFileException -> {}
                // a directory its owner may not read cannot even be listed: open it up and walk it again
                exc is AccessDeniedException && makeAccessible(file) -> deleteRecursively(file)
                else -> throw exc
            }
            return FileVisitResult.CONTINUE
        }

        override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
            if (exc != null) throw exc
            delete(dir)
            return FileVisitResult.CONTINUE
        }
    })
}

/**
 * Gives the owner full access to [dir] and tells whether that changed anything. Archives may hold read-only directories,
 * and nothing can be deleted from one until its owner may write to it.
 */
private fun makeAccessible(dir: Path): Boolean {
    val attributes = try {
        Files.readAttributes(dir, PosixFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS)
    } catch (e: UnsupportedOperationException) {
        // not a POSIX file system
        return false
    }
    if (!attributes.isDirectory || attributes.permissions().containsAll(OWNER_ACCESS)) return false
    // Not with NOFOLLOW_LINKS: the JDK then opens the directory to change it, which is the very thing that is denied.
    // It is a directory and not a link, as checked above.
    Files.setPosixFilePermissions(dir, attributes.permissions() + OWNER_ACCESS)
    return true
}

private fun delete(path: Path) {
    try {
        Files.deleteIfExists(path)
    } catch (e: AccessDeniedException) {
        // Windows refuses to delete read-only files, and archives often contain them
        val dosView = Files.getFileAttributeView(path, DosFileAttributeView::class.java, LinkOption.NOFOLLOW_LINKS) ?: throw e
        dosView.setReadOnly(false)
        Files.deleteIfExists(path)
    }
}
