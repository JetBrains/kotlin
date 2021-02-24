/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.file

import java.net.URI
import java.nio.file.*
import java.nio.file.spi.FileSystemProvider

// Zip filesystem provider doesn't allow creating several instances of ZipFileSystem from the same URI,
// so newFileSystem(URI, ...) throws a FileSystemAlreadyExistsException in this case.
// But FileSystemProvider.newFileSystem(File, ...) cannot throw this exception and allows creating several filesystems.
// See also:
// https://bugs.java.com/bugdatabase/view_bug.do?bug_id=7001822
// https://bugs.java.com/bugdatabase/view_bug.do?bug_id=6994161
fun File.zipFileSystem(create: Boolean = false): FileSystem {
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

fun Path.unzipTo(directory: Path) {
    val zipUri = URI.create("jar:" + this.toUri())
    FileSystems.newFileSystem(zipUri, emptyMap<String, Any?>(), null).use { zipfs ->
        val zipPath = zipfs.getPath("/")
        zipPath.recursiveCopyTo(directory)
    }
}

fun <T> File.withZipFileSystem(create: Boolean, action: (FileSystem) -> T): T {
    return this.zipFileSystem(create).use(action)
}

fun <T> File.withZipFileSystem(action: (FileSystem) -> T): T = this.withZipFileSystem(false, action)
