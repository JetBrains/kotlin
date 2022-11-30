package org.jetbrains.kotlin.library

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.file.unzipTo
import org.jetbrains.kotlin.library.impl.zippedKotlinLibraryChecks

const val KLIB_FILE_EXTENSION = "klib"
const val KLIB_FILE_EXTENSION_WITH_DOT = ".$KLIB_FILE_EXTENSION"

const val KLIB_METADATA_FILE_EXTENSION = "knm"
const val KLIB_METADATA_FILE_EXTENSION_WITH_DOT = ".$KLIB_METADATA_FILE_EXTENSION"

fun File.unpackZippedKonanLibraryTo(newDir: File) {

    // First, run validity checks for the given KLIB file.
    zippedKotlinLibraryChecks(this)

    if (newDir.exists) {
        if (newDir.isDirectory)
            newDir.deleteRecursively()
        else
            newDir.delete()
    }

    this.unzipTo(newDir)
    check(newDir.exists) { "Could not unpack $this as $newDir." }
}

val List<String>.toUnresolvedLibraries
    get() = this.map {
        UnresolvedLibrary(it, null)
    }
