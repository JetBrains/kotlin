/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library

import org.jetbrains.kotlin.io.ZipFileSystemAccessor
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Files.createTempDirectory
import java.nio.file.Files.createTempFile
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.zip.ZipException
import kotlin.io.path.copyTo
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

/**
 * A class that allows reading from a specific [KlibComponent] using the corresponding [KlibComponentLayout].
 */
sealed class KlibLayoutReader<KCL : KlibComponentLayout> {
    abstract fun <T> readInPlace(readAction: (KCL) -> T): T

    fun <T> readInPlaceOrFallback(fallbackValueInCaseOfException: T, readAction: (KCL) -> T): T =
        try {
            readInPlace(readAction)
        } catch (_: IOException) {
            fallbackValueInCaseOfException
        }

    abstract fun readExtractingToTemp(readAction: (KCL) -> Path): Path

    /**
     * Read from a directory on the file system.
     * Use the [klibDir] parameter as the [KlibComponentLayout.root].
     *
     * @param klibDir The Klib directory.
     * @param layoutBuilder A function that builds the [KlibComponentLayout].
     */
    class FromDirectory<KCL : KlibComponentLayout>(klibDir: Path, layoutBuilder: (Path) -> KCL) : KlibLayoutReader<KCL>() {
        private val layout = layoutBuilder(klibDir)
        override fun <T> readInPlace(readAction: (KCL) -> T): T = readAction(layout)
        override fun readExtractingToTemp(readAction: (KCL) -> Path): Path = readAction(layout)
    }

    /**
     * Read from a ZIP archive through a virtual file system.
     *
     * The virtual file system is created only on demand, when [readInPlace] is called.
     * Since we can't reason about whether the virtual file system will be cached for later reuse or not,
     * because that's controlled by the exact implementation of [ZipFileSystemAccessor], we have to make sure
     * that there are no links to the virtual file system left after the [readInPlace] call.
     *
     * Therefore, the instance of [KlibComponentLayout] is also created on demand and not cached anywhere.
     *
     * @param klibArchive The Klib archive.
     * @param zipFileSystemAccessor The [ZipFileSystemAccessor] to use.
     * @param layoutBuilder A function that builds the [KlibComponentLayout].
     */
    class FromZipArchive<KCL : KlibComponentLayout>(
        private val klibArchive: Path,
        private val zipFileSystemAccessor: ZipFileSystemAccessor,
        private val layoutBuilder: (Path) -> KCL
    ) : KlibLayoutReader<KCL>() {
        override fun <T> readInPlace(readAction: (KCL) -> T): T =
            zipFileSystemAccessor.withZipFileSystem(klibArchive) { zipFileSystem ->
                readAction(layoutBuilder(zipFileSystem.getPath("/")))
            }

        override fun readExtractingToTemp(readAction: (KCL) -> Path): Path = readInPlace { layout ->
            val fileOrDirectory = readAction(layout)
            when {
                fileOrDirectory.isDirectory() -> {
                    val tempDir = createTempDirectory(fileOrDirectory.name)
                    tempDir.deleteOnExitRecursively()
                    fileOrDirectory.listDirectoryEntries().forEach { file -> file.copyTo(tempDir.resolve(file.name)) }
                    tempDir
                }

                fileOrDirectory.isRegularFile() -> {
                    val tempFile = createTempFile(fileOrDirectory.name, null)
                    tempFile.deleteOnExitRecursively()
                    fileOrDirectory.copyTo(tempFile, overwrite = true)
                    tempFile
                }

                fileOrDirectory.exists() -> throw ZipException("Non-existing file or directory in KLIB archive: $fileOrDirectory")

                else -> throw ZipException("Unsupported type of the file system object in KLIB archive: $fileOrDirectory")
            }
        }
    }

    companion object {
        private fun Path.deleteOnExitRecursively() {
            if (!exists()) return

            Files.walkFileTree(this, object : SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    file.toFile().deleteOnExit()
                    return FileVisitResult.CONTINUE
                }

                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                    dir.toFile().deleteOnExit()
                    return FileVisitResult.CONTINUE
                }
            })
        }

    }
}

/**
 * The factory that allows creating instances of [KlibLayoutReader] for specific [KlibComponent]s.
 */
class KlibLayoutReaderFactory(
    private val klibFile: Path,
    private val zipFileSystemAccessor: ZipFileSystemAccessor,
) {
    fun <KCL : KlibComponentLayout> createLayoutReader(layoutBuilder: (Path) -> KCL): KlibLayoutReader<KCL> {
        return when (KlibFormat.guessBy(klibFile)) {
            KlibFormat.ZipArchive -> KlibLayoutReader.FromZipArchive(
                klibArchive = klibFile,
                zipFileSystemAccessor = zipFileSystemAccessor,
                layoutBuilder = layoutBuilder
            )

            KlibFormat.Directory -> KlibLayoutReader.FromDirectory(
                klibDir = klibFile,
                layoutBuilder = layoutBuilder
            )
        }
    }
}

