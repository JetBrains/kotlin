package org.jetbrains.kotlin.library

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.file.file
import org.jetbrains.kotlin.konan.file.withMutableZipFileSystem
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

    this.withMutableZipFileSystem {
        it.file("/").recursiveCopyTo(newDir)
    }
    check(newDir.exists) { "Could not unpack $this as $newDir." }
}

val List<String>.toUnresolvedLibraries
    get() = this.map {

        val version = it.substringAfterLast('@', "")
            .let { if (it.isEmpty()) null else it }
        val name = it.substringBeforeLast('@')
        UnresolvedLibrary(name, version)
    }
