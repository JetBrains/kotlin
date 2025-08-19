/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package common

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest


/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

fun buildAbsPath(pathSuffix: String): String {
    val projectRoot = System.getProperty("user.dir")
    return "$projectRoot/$pathSuffix"
}

fun computeSha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    if (file.isDirectory){
        file.walkTopDown().forEach { fileInDir->
            fileInDir.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
        }
    }else{
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
    }

    return digest.digest().joinToString("") { "%02x".format(it) }
}

fun computeSha256(fileChunks: List<ByteArray>): String {
    val digest = MessageDigest.getInstance("SHA-256")
    fileChunks.forEach { chunk ->
        digest.update(chunk)
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

fun copyDirectoryRecursively(source: Path, target: Path, overwrite: Boolean = false) {
    val copyOptions = if (overwrite) {
        arrayOf(StandardCopyOption.REPLACE_EXISTING)
    } else {
        emptyArray()
    }

    Files.createDirectories(target)
    Files.walk(source).use { paths ->
        paths.forEach { path ->
            val destination = target.resolve(source.relativize(path))
            if (Files.isDirectory(path)) {
                Files.createDirectories(destination)
            } else {
                Files.copy(path, destination, *copyOptions)
            }
        }
    }
}