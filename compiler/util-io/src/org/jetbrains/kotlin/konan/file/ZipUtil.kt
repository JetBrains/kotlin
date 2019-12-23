/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.file

import java.net.URI
import java.nio.file.*
import java.util.concurrent.ConcurrentHashMap

private val File.zipUri: URI
    get() = URI.create("jar:${this.toPath().toUri()}")

private val counters = ConcurrentHashMap<FileSystem, Int>()

fun File.zipFileSystem(mutable: Boolean = false): FileSystem {
    val zipUri = this.zipUri
    val attributes = hashMapOf("create" to mutable.toString())
    return try {
        FileSystems.newFileSystem(zipUri, attributes, null).also {
            counters[it] = 1
        }
    } catch (e: FileSystemAlreadyExistsException) {
        FileSystems.getFileSystem(zipUri).also {
            counters.computeIfPresent(it, { t, u -> u + 1})
        }
    }
}

fun FileSystem.file(file: File) = File(this.getPath(file.path))

fun FileSystem.file(path: String) = File(this.getPath(path))

private fun File.toPath() = Paths.get(this.path)

fun File.zipDirAs(unixFile: File) {
    unixFile.withMutableZipFileSystem {
        this.recursiveCopyTo(it.file("/"))
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
        val counter = counters[zipFileSystem]!!
        if (counter == 1) {
            zipFileSystem.close()
            counters.remove(zipFileSystem)
        } else {
            counters.computeIfPresent(zipFileSystem, { t, u -> u - 1})
        }
    }
}

fun <T> File.withZipFileSystem(action: (FileSystem) -> T): T = this.withZipFileSystem(false, action)

fun <T> File.withMutableZipFileSystem(action: (FileSystem) -> T): T = this.withZipFileSystem(true, action)