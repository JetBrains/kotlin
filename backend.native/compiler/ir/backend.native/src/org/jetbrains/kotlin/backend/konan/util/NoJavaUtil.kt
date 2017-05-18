/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan.util

import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

class File(val path: String) {
    private constructor(file: java.io.File): this(file.path)
    constructor(parent: String, child: String): this(java.io.File(parent, child))
    constructor(parent: File, child: String): this(java.io.File(parent.path, child))

    private val javaFile = java.io.File(path)
    val absolutePath: String
        get() = javaFile.absolutePath
    val absoluteFile: File
        get() = File(absolutePath)
    val name: String
        get() = javaFile.name
    val parent: String
        get() = javaFile.parent

    val exists 
        get() = javaFile.exists()
    val isDirectory 
        get() = javaFile.isDirectory()
    val isFile 
        get() = javaFile.isFile()
    val listFiles
        get() = javaFile.listFiles()!!.toList()

    fun mkdirs() = javaFile.mkdirs()
    fun delete() = javaFile.delete()
    fun deleteRecursively() = javaFile.deleteRecursively()
    fun readText() = javaFile.readText()
    fun writeText(text: String) = javaFile.writeText(text)

    override fun toString() = path

    // TODO: Consider removeing these after konanazing java.util.Properties.
    fun bufferedReader() = javaFile.bufferedReader()
    fun outputStream() = javaFile.outputStream()
}


private val File.zipUri: URI
        get() = URI.create("jar:file:${this.absolutePath}")

private val File.zipRootPath: Path
    get() {
        val zipUri = this.zipUri
        val allowCreation = hashMapOf("create" to "true")
        val zipfs = FileSystems.newFileSystem(zipUri, allowCreation, null)
        return zipfs.getPath("/")
    }

private fun File.toPath() = Paths.get(this.path)

fun File.zipDirAs(zipFile: File) {
    val zipPath = zipFile.zipRootPath
    this.toPath().recursiveCopyTo(zipPath)
    zipPath.fileSystem.close()
}

fun File.unzipAs(directory: File) {
    val zipPath = this.zipRootPath
    zipPath.recursiveCopyTo(directory.toPath())
    zipPath.fileSystem.close()
}

fun Path.recursiveCopyTo(destPath: Path) {
    val sourcePath = this
    Files.walk(sourcePath).forEach next@ { oldPath ->

        val relative = sourcePath.relativize(oldPath)
        val destFs = destPath.getFileSystem()
        // We are copying files between file systems, 
        // so pass the relative path through the Sting.
        val newPath = destFs.getPath(destPath.toString(), relative.toString())

        // File systems don't allow replacing an existing root.
        if (newPath == newPath.getRoot()) return@next 
        Files.copy(oldPath, newPath,
            StandardCopyOption.REPLACE_EXISTING)
    }
}

fun File.copyTo(destination: File) {
    Files.copy(this.toPath(), destination.toPath()) 
}

