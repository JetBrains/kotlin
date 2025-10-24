/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library

import org.jetbrains.kotlin.konan.file.File as KlibFile
import org.jetbrains.kotlin.konan.file.ZipFileSystemAccessor
import org.jetbrains.kotlin.konan.file.file

/**
 * A class that allows reading from a specific [KlibComponent] using the corresponding [KlibComponent.Layout].
 */
sealed interface KlibLayoutReader<KCL : KlibComponent.Layout> {
    fun <T> readInPlace(readAction: (KCL) -> T): T

    /**
     * Read from a directory on the file system.
     * Use the [klibDir] parameter as the [KlibComponent.Layout.root].
     *
     * @param klibDir The Klib directory.
     * @param layoutBuilder A function that builds the [KlibComponent.Layout].
     */
    class FromDirectory<KCL : KlibComponent.Layout>(klibDir: KlibFile, layoutBuilder: (KlibFile) -> KCL) : KlibLayoutReader<KCL> {
        private val layout = layoutBuilder(klibDir)
        override fun <T> readInPlace(readAction: (KCL) -> T): T = readAction(layout)
    }

    /**
     * Read from a ZIP archive through a virtual file system.
     *
     * The virtual file system is created only on demand, when [readInPlace] is called.
     * Since we can't reason about whether the virtual file system will be cached for later reuse or not,
     * because that's controlled by the exact implementation of [ZipFileSystemAccessor], we have to make sure
     * that there are no links to the virtual file system left after the [readInPlace] call.
     *
     * Therefore, the instance of [KlibComponent.Layout] is also created on demand and not cached anywhere.
     *
     * @param klibArchive The Klib archive.
     * @param zipFileSystemAccessor The [ZipFileSystemAccessor] to use.
     * @param layoutBuilder A function that builds the [KlibComponent.Layout].
     */
    class FromZipArchive<KCL : KlibComponent.Layout>(
        private val klibArchive: KlibFile,
        private val zipFileSystemAccessor: ZipFileSystemAccessor,
        private val layoutBuilder: (KlibFile) -> KCL
    ) : KlibLayoutReader<KCL> {
        override fun <T> readInPlace(readAction: (KCL) -> T): T =
            zipFileSystemAccessor.withZipFileSystem(klibArchive) { zipFileSystem ->
                readAction(layoutBuilder(zipFileSystem.file("/")))
            }
    }
}

/**
 * The factory that allows creating instances of [KlibLayoutReader] for specific [KlibComponent]s.
 */
class KlibLayoutReaderFactory(
    private val klibFile: KlibFile,
    private val zipFileSystemAccessor: ZipFileSystemAccessor,
) {
    fun <KCL : KlibComponent.Layout> createLayoutReader(layoutBuilder: (KlibFile) -> KCL): KlibLayoutReader<KCL> {
        return if (klibFile.isFile) {
            KlibLayoutReader.FromZipArchive(
                klibArchive = klibFile,
                zipFileSystemAccessor = zipFileSystemAccessor,
                layoutBuilder = layoutBuilder
            )
        } else {
            KlibLayoutReader.FromDirectory(
                klibDir = klibFile,
                layoutBuilder = layoutBuilder
            )
        }
    }
}

