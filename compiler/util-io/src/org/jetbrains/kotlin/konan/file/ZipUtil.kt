/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.file

import java.net.URI
import java.nio.file.*
import java.util.concurrent.ConcurrentHashMap

private val File.zipUri: URI
    get() = URI.create("jar:${canonicalFile.toPath().toUri()}")

private data class FileSystemRefCounter(val fileSystem: FileSystem, val counter: Int)

private val fileSystems = ConcurrentHashMap<URI, FileSystemRefCounter>()

// Zip filesystem provider creates a singleton zip FileSystem.
// So newFileSystem can return an already existing one.
// And, more painful, closing the filesystem could close it for another consumer thread.
fun File.zipFileSystem(mutable: Boolean = false): FileSystem {
    val zipUri = this.zipUri
    val attributes = hashMapOf("create" to mutable.toString())

    return fileSystems.compute(zipUri) { key, value ->
        if (value == null) {
            FileSystemRefCounter(FileSystems.newFileSystem(key, attributes, null), 1)
        } else {
            // TODO: If a file system already exists, we cannot change its mutability.
            FileSystemRefCounter(value.fileSystem, value.counter + 1)
        }
    }!!.fileSystem
}

fun FileSystem.file(file: File) = File(this.getPath(file.path))

fun FileSystem.file(path: String) = File(this.getPath(path))

private fun File.toPath() = Paths.get(this.path)

fun File.zipDirAs(unixFile: File) {
    unixFile.withMutableZipFileSystem {
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

fun <T> File.withZipFileSystem(mutable: Boolean = false, action: (FileSystem) -> T): T {
    val zipFileSystem = this.zipFileSystem(mutable)
    return try {
        action(zipFileSystem)
    } finally {
        fileSystems.compute(zipUri) { _, value ->
            require(value != null)
            if (value.counter == 1) {
                value.fileSystem.close()
                // Returning null removes this entry from the map
                // See https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ConcurrentMap.html.
                null
            } else {
                FileSystemRefCounter(value.fileSystem, value.counter - 1)
            }
        }
    }
}

fun <T> File.withZipFileSystem(action: (FileSystem) -> T): T = this.withZipFileSystem(false, action)

fun <T> File.withMutableZipFileSystem(action: (FileSystem) -> T): T = this.withZipFileSystem(true, action)