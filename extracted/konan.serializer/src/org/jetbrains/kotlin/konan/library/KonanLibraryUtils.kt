package org.jetbrains.kotlin.konan.library

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.impl.DefaultMetadataReaderImpl
import org.jetbrains.kotlin.konan.library.impl.KonanLibraryImpl
import org.jetbrains.kotlin.konan.library.impl.zippedKonanLibraryChecks
import org.jetbrains.kotlin.konan.library.impl.zippedKonanLibraryRoot
import org.jetbrains.kotlin.konan.target.KonanTarget

fun File.unpackZippedKonanLibraryTo(newDir: File) {

    // first run validity checks for the given KLIB file
    zippedKonanLibraryChecks(this)

    if (newDir.exists) {
        if (newDir.isDirectory)
            newDir.deleteRecursively()
        else
            newDir.delete()
    }

    zippedKonanLibraryRoot(this).recursiveCopyTo(newDir)
    check(newDir.exists) { "Could not unpack $this as $newDir." }
}

fun createKonanLibraryReader(
        libraryFile: File,
        currentAbiVersion: Int,
        target: KonanTarget? = null,
        isDefaultLibrary: Boolean = false,
        metadataReader: MetadataReader = DefaultMetadataReaderImpl
): KonanLibrary = KonanLibraryImpl(libraryFile, currentAbiVersion, target, isDefaultLibrary, metadataReader)
