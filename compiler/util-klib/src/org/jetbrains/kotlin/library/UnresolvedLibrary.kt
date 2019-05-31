package org.jetbrains.kotlin.library

data class UnresolvedLibrary(
    val path: String,
    val libraryVersion: String?) {

    fun substitutePath(newPath: String): UnresolvedLibrary {
        return UnresolvedLibrary(newPath, libraryVersion)
    }
}