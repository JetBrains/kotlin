/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.file

import org.jetbrains.kotlin.util.removeSuffixIfPresent
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.PrintWriter
import java.io.RandomAccessFile
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes

data class File(internal val javaPath: Path) {
    constructor(parent: Path, child: String) : this(parent.resolve(child))
    constructor(parent: File, child: String) : this(parent.javaPath.resolve(child))
    constructor(parent: File, child: File) : this(parent.javaPath.resolve(child.javaPath))
    constructor(path: String) : this(Paths.get(path))
    constructor(parent: String, child: String) : this(Paths.get(parent, child))

    val path: String
        get() = javaPath.toString()
    val absolutePath: String
        get() = javaPath.toAbsolutePath().toString()
    val absoluteFile: File
        get() = File(absolutePath)
    val canonicalPath: String by lazy {
        javaPath.toFile().canonicalPath
    }
    val canonicalFile: File
        get() = File(canonicalPath)

    val name: String
        get() = javaPath.fileName.toString().removeSuffixIfPresent(separator) // https://bugs.java.com/bugdatabase/view_bug.do?bug_id=8153248
    val extension: String
        get() = name.substringAfterLast('.', "")
    val nameSegments: List<String>
        get() = javaPath.map { it.fileName.toString() }
    val parent: String
        get() = javaPath.parent.toString()
    val parentFile: File
        get() = File(javaPath.parent)

    val exists: Boolean
        get() = Files.exists(javaPath)
    val isDirectory: Boolean
        get() = Files.isDirectory(javaPath)
    val isFile: Boolean
        get() = Files.isRegularFile(javaPath)
    val isAbsolute: Boolean
        get() = javaPath.isAbsolute
    val listFiles: List<File>
        get() = Files.newDirectoryStream(javaPath).use { stream -> stream.map(::File) }
    val listFilesOrEmpty: List<File>
        get() = if (exists) listFiles else emptyList()

    // A fileKey is an object that uniquely identifies the given file.
    val fileKey: Any
        get() {
            // It is not guaranteed that all filesystems have fileKey. If not we fall
            // back on canonicalPath which can be significantly slower to get.
            var key = Files.readAttributes(javaPath, BasicFileAttributes::class.java).fileKey()
            if (key == null) {
                key = this.canonicalPath
            }
            return key
        }

    val size: Long get() = Files.size(javaPath)

    fun child(name: String): File = File(this, name)
    fun startsWith(another: File): Boolean = javaPath.startsWith(another.javaPath)

    fun copyTo(destination: File) {
        Files.copy(javaPath, destination.javaPath, StandardCopyOption.REPLACE_EXISTING)
    }

    fun renameTo(destination: File): Boolean = javaPath.toFile().renameTo(destination.javaPath.toFile())

    fun mkdirs(): Path = Files.createDirectories(javaPath)
    fun delete(): Boolean = Files.deleteIfExists(javaPath)
    fun deleteRecursively(): Unit = postorder { Files.delete(it) }
    fun deleteOnExitRecursively(): Unit = preorder { File(it).deleteOnExit() }

    fun preorder(task: (Path) -> Unit) {
        if (!this.exists) return

        Files.walkFileTree(javaPath, object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                task(file)
                return FileVisitResult.CONTINUE
            }

            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                task(dir)
                return FileVisitResult.CONTINUE
            }
        })

    }

    fun postorder(task: (Path) -> Unit) {
        if (!this.exists) return

        Files.walkFileTree(javaPath, object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                task(file)
                return FileVisitResult.CONTINUE
            }

            override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                task(dir)
                return FileVisitResult.CONTINUE
            }
        })
    }

    fun map(
        mode: FileChannel.MapMode = FileChannel.MapMode.READ_ONLY,
        start: Long = 0, size: Long = -1,
    ): MappedByteBuffer {
        val file = RandomAccessFile(
            path,
            if (mode == FileChannel.MapMode.READ_ONLY) "r" else "rw"
        )
        val fileSize = if (mode == FileChannel.MapMode.READ_ONLY)
            file.length() else size.also { assert(size != -1L) }
        val channel = file.channel
        return channel.map(mode, start, fileSize).also { channel.close() }
    }

    fun deleteOnExit(): File {
        // Works only on the default file system,
        // but that's okay for now.
        javaPath.toFile().deleteOnExit()
        return this // Allow streaming.
    }

    fun createNew(): Boolean = javaPath.toFile().createNewFile()

    fun readBytes(): ByteArray = Files.readAllBytes(javaPath)
    fun writeBytes(bytes: ByteArray): Path = Files.write(javaPath, bytes)
    fun appendBytes(bytes: ByteArray): Path = Files.write(javaPath, bytes, StandardOpenOption.APPEND)

    fun writeLines(lines: Iterable<String>) {
        Files.write(javaPath, lines)
    }

    fun writeText(text: String): Unit = writeLines(listOf(text))

    fun appendLines(lines: Iterable<String>) {
        Files.write(javaPath, lines, StandardOpenOption.APPEND)
    }

    fun appendText(text: String): Unit = appendLines(listOf(text))

    fun forEachLine(action: (String) -> Unit) {
        Files.lines(javaPath).use { lines ->
            lines.forEach { action(it) }
        }
    }

    fun createAsSymlink(target: String) {
        val targetPath = Paths.get(target)
        if (Files.isSymbolicLink(this.javaPath) && Files.readSymbolicLink(javaPath) == targetPath) {
            return
        }
        Files.createSymbolicLink(this.javaPath, targetPath)
    }

    override fun toString(): String = path

    // TODO: Consider removeing these after konanazing java.util.Properties.
    fun bufferedReader(): BufferedReader = Files.newBufferedReader(javaPath)
    fun outputStream(): OutputStream = Files.newOutputStream(javaPath)
    fun printWriter(): PrintWriter = javaPath.toFile().printWriter()

    companion object {
        val userDir: File
            get() = File(System.getProperty("user.dir"))

        val userHome: File
            get() = File(System.getProperty("user.home"))

        val javaHome: File
            get() = File(System.getProperty("java.home"))
        val pathSeparator: String = java.io.File.pathSeparator
        val separator: String = java.io.File.separator
        val separatorChar: Char = java.io.File.separatorChar
    }

    fun readStrings(): MutableList<String> = mutableListOf<String>().also { list -> forEachLine { list.add(it) } }

    override fun equals(other: Any?): Boolean {
        val otherFile = other as? File ?: return false
        return otherFile.javaPath.toAbsolutePath() == javaPath.toAbsolutePath()
    }

    override fun hashCode(): Int = javaPath.toAbsolutePath().hashCode()
}

fun String.File(): File = File(this)
fun Path.File(): File = File(this)

fun createTempFile(name: String, suffix: String? = null): File = Files.createTempFile(name, suffix).File()
fun createTempDir(name: String): File = Files.createTempDirectory(name).File()

fun bufferedReader(errorStream: InputStream): BufferedReader = BufferedReader(InputStreamReader(errorStream))

// stdlib `use` function adapted for AutoCloseable.
inline fun <T : AutoCloseable?, R> T.use(block: (T) -> R): R {
    var closed = false
    try {
        return block(this)
    } catch (e: Exception) {
        closed = true
        try {
            this?.close()
        } catch (closeException: Exception) {
        }
        throw e
    } finally {
        if (!closed) {
            this?.close()
        }
    }
}
