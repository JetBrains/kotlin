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

package org.jetbrains.kotlin.backend.konan

import java.io.File
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

val File.zipUri: URI
        get() = URI.create("jar:file:${this.absolutePath}")

val File.zipRootPath: Path
    get() {
        val zipUri = this.zipUri
        val allowCreation = hashMapOf("create" to "true")
        val zipfs = FileSystems.newFileSystem(zipUri, allowCreation, null)
        return zipfs.getPath("/")
    }

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

