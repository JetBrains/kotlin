/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.file

import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributeView
import java.nio.file.attribute.FileTime
import java.nio.file.spi.FileSystemProvider
import java.util.zip.ZipException

// Zip filesystem provider doesn't allow creating several instances of ZipFileSystem from the same URI,
// so newFileSystem(URI, ...) throws a FileSystemAlreadyExistsException in this case.
// But FileSystemProvider.newFileSystem(File, ...) cannot throw this exception and allows creating several filesystems.
// See also:
// https://bugs.java.com/bugdatabase/view_bug.do?bug_id=7001822
// https://bugs.java.com/bugdatabase/view_bug.do?bug_id=6994161
private fun File.zipFileSystem(create: Boolean = false): FileSystem {
    val attributes = hashMapOf("create" to create.toString())

    // There is no FileSystems.newFileSystem overload accepting the attribute map.
    // So we have to manually iterate over the filesystem providers.
    return FileSystemProvider.installedProviders().filter { it.scheme == "jar" }.mapNotNull {
        try {
            it.newFileSystem(this.toPath(), attributes)
        } catch(e: Exception) {
            when(e) {
                is UnsupportedOperationException,
                is IllegalArgumentException -> null
                else -> throw e
            }
        }
    }.first()
}

fun FileSystem.file(file: File) = File(this.getPath(file.path))

fun FileSystem.file(path: String) = File(this.getPath(path))

private fun File.toPath() = Paths.get(this.path)

fun File.zipDirAs(unixFile: File) {
    unixFile.withZipFileSystem(create = true) {
        // Time attributes are not preserved to ensure that the output zip file bytes deterministic for a fixed set of
        // input files.
        this.recursiveCopyTo(it.file("/"), resetTimeAttributes = true)
    }
}

/**
 * Unpacks the contents of a zip archive located in [this] into the [destinationDirectory].
 *
 * @param destinationDirectory The directory to unpack the contents to.
 * @param resetTimeAttributes Whether to set the newly created files' time attributes
 * (creation time, last access time, and last modification time) to zero.
 * @param fromSubdirectory A subdirectory inside the archive to unpack. Specify "/" if you need to unpack the whole archive.
 */
fun File.unzipTo(destinationDirectory: File, fromSubdirectory: File = File("/"), resetTimeAttributes: Boolean = false) {
    withZipFileSystem {
        it.file(fromSubdirectory).recursiveCopyTo(destinationDirectory, resetTimeAttributes)
    }
}

/**
 * Unpacks the contents of a zip archive located in [this] into the [destinationDirectory].
 *
 * @param destinationDirectory The directory to unpack the contents to.
 * @param resetTimeAttributes Whether to set the newly created files' time attributes
 * (creation time, last access time, and last modification time) to zero.
 * @param fromSubdirectory A subdirectory inside the archive to unpack. Specify "/" if you need to unpack the whole archive.
 */
fun Path.unzipTo(destinationDirectory: Path, fromSubdirectory: Path = Paths.get("/"), resetTimeAttributes: Boolean = false) {
    File(this).unzipTo(File(destinationDirectory), File(fromSubdirectory), resetTimeAttributes)
}

fun <T> File.withZipFileSystem(create: Boolean, action: (FileSystem) -> T): T {
    return this.zipFileSystem(create).use(action)
}

fun <T> File.withZipFileSystem(action: (FileSystem) -> T): T = this.withZipFileSystem(false, action)

private fun File.recursiveCopyTo(destination: File, resetTimeAttributes: Boolean = false) {
    val sourcePath = javaPath
    val destPath = destination.javaPath
    val destFs = destPath.fileSystem
    val normalizedDestPath = destPath.normalize()
    Files.walk(sourcePath).forEach next@{ oldPath ->

        val relative = sourcePath.relativize(oldPath)

        // We are copying files between file systems,
        // so pass the relative path through the String.
        val newPath = destFs.getPath(destPath.toString(), relative.toString())

        // NOTE: this check is important, it prevents a potential ZipSlip vulnerability
        if (!newPath.normalize().startsWith(normalizedDestPath)) {
            throw ZipException("$relative attempted to escape the destination directory $destination")
        }

        // File systems don't allow replacing an existing root.
        if (newPath == newPath.root) return@next
        if (Files.isDirectory(newPath)) {
            Files.createDirectories(newPath)
        } else {
            Files.copy(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING)
        }
        if (resetTimeAttributes) {
            val zero = FileTime.fromMillis(0)
            Files.getFileAttributeView(newPath, BasicFileAttributeView::class.java).setTimes(zero, zero, zero);
        }
    }
}
