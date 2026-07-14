/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.io

import java.io.File
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.createSymbolicLinkPointingTo
import kotlin.io.path.exists
import kotlin.io.path.isSymbolicLink
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readAttributes
import kotlin.io.path.readSymbolicLink

/**
 * Returns a canonical path computed the same way as [File.canonicalPath] does.
 */
fun Path.canonicalPathString(): String = toFile().canonicalPath

/**
 * Returns a 'fileKey': An object that uniquely identifies the given file.
 */
fun Path.fileKey(): Any {
    // It is not guaranteed that all filesystems have fileKey. If not we fall
    // back on canonicalPathString which can be significantly slower to get.
    return readAttributes<BasicFileAttributes>().fileKey() ?: canonicalPathString()
}

/**
 * Registers the file or (empty) directory to be deleted when the JVM terminates.
 * Note: Files (or directories) are deleted in the reverse order that they are registered.
 */
fun Path.deleteOnExit() {
    toFile().deleteOnExit()
}

/**
 * Registers the file or (potentially non-empty) directory to be deleted when the JVM terminates.
 * Note: All underlying files and directories will be deleted before deleting the containing directory.
 */
fun Path.deleteOnExitRecursively() {
    if (!exists()) return

    Files.walkFileTree(this, object : SimpleFileVisitor<Path>() {
        override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
            file.deleteOnExit()
            return FileVisitResult.CONTINUE
        }

        override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
            dir.deleteOnExit()
            return FileVisitResult.CONTINUE
        }
    })
}

/**
 * A safe version of [Path.listDirectoryEntries] that does not fail and returns `false` if the directory
 * represented by the extension receiver does not exist.
 */
fun Path.listDirectoryEntriesIfDirectoryExists(): List<Path> = if (exists()) listDirectoryEntries() else emptyList()

/**
 * A safe version of [Path.createSymbolicLinkPointingTo] that does not to attempt to create a symbolic link
 * if the required symbolic link already exists.
 */
fun Path.ensureSymbolicLinkTo(target: Path) {
    if (!isSymbolicLink() || readSymbolicLink() != target) {
        createSymbolicLinkPointingTo(target)
    }
}
