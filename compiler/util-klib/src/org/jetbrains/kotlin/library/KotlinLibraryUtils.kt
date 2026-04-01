package org.jetbrains.kotlin.library

val List<String>.toUnresolvedLibraries
    get() = this.map {
        RequiredUnresolvedLibrary(it)
    }
