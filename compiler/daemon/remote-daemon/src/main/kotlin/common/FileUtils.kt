/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package common

import model.FileChunk
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.jetbrains.kotlin.wasm.ir.convertors.ByteWriter
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest


/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


fun cleanCompilationResultPath(path: String): String {
    // - cache/artifacts/<any>/
    // - workspace/<userId>/<projectName>/output/<moduleName>/
    val patterns = listOf(
        Regex("""^.*?/cache/artifacts/[^/]+/"""),
        Regex("""^.*?/workspace/[^/]+/[^/]+/output/[^/]+/""")
    )
    var cleaned = path
    for (rx in patterns) {
        val match = rx.find(cleaned)
        if (match != null && match.range.first == 0) {
            cleaned = cleaned.removeRange(match.range)
            break
        }
    }
    return cleaned
}



fun computeSha256(file: File): String {
    // TODO: in a real world scenario we should use something more robust
    // maybe we could use a special library for hashing
    // double check the hashing of directories, there is not a single approach
    // how to hash directories
    // for example, in the current implementation, the same path with forward slashes and back slashes
    // will be hashed differently and that's and issue
    val digest = MessageDigest.getInstance("SHA-256")
    if (file.isDirectory) {
        file.walkTopDown()
            .filter { it.isFile }
            .sortedBy { it.relativeTo(file).path }
            .forEach { childFile ->

                val relativePath = childFile.relativeTo(file).path
                digest.update(relativePath.toByteArray(Charsets.UTF_8))

                childFile.inputStream().use { input ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        digest.update(buffer, 0, bytesRead)
                    }
                }
            }
    } else {
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

fun copyDirectoryRecursively(source: Path, target: Path, overwrite: Boolean = false): File {
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
    return target.toFile()
}

fun createTarArchiveStream(sourceDir: File): InputStream {
    // TODO: performance improvement
    // we would like to avoid creating file on disk, instead of that we can just stream it directly to a chunker
    // I tried to implement it using PipedInputStream and PipedOutputStream, but it didn't worked properly...
    val tempTar = kotlin.io.path.createTempFile("archive-", ".tar").toFile().apply { deleteOnExit() }
    FileOutputStream(tempTar).use { fos ->
        TarArchiveOutputStream(BufferedOutputStream(fos)).use { tarOut ->
            tarOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)
            sourceDir.walkTopDown().forEach { file ->
                if (file == sourceDir) return@forEach
                val entryName = sourceDir.toPath().relativize(file.toPath()).toString()
                val entry = TarArchiveEntry(file, entryName).apply {
                    if (file.isFile) size = file.length()
                }
                tarOut.putArchiveEntry(entry)
                if (file.isFile) {
                    file.inputStream().use { it.copyTo(tarOut) }
                }
                tarOut.closeArchiveEntry()
            }
            tarOut.finish()
        }
    }
    return FileInputStream(tempTar)
}


fun extractTarArchive(sourceTar: File, destDir: File) {
    destDir.mkdirs()
    TarArchiveInputStream(FileInputStream(sourceTar)).use { tarIn ->
        var entry = tarIn.nextEntry

        while (entry != null) {
            val destPath = File(destDir, entry.name)

            if (entry.isDirectory) {
                destPath.mkdirs()
            } else {
                destPath.parentFile.mkdirs()
                destPath.outputStream().use {
                    tarIn.copyTo(it)
                }
            }
            entry = tarIn.nextEntry
        }
    }
}