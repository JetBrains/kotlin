package org.jetbrains.kotlin.library

const val KLIB_FILE_EXTENSION = "klib"
const val KLIB_FILE_EXTENSION_WITH_DOT = ".$KLIB_FILE_EXTENSION"

val List<String>.toUnresolvedLibraries
    get() = this.map {
        RequiredUnresolvedLibrary(it)
    }
