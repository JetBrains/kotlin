// LANGUAGE: +FullValueClasses
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
package library

sealed value class Result {
    abstract val code: Int
}

value object Empty : Result() {
    override val code: Int get() = 0
}

// MODULE: main(lib)
// FILE: main.kt
package test

import library.Empty

fun usage() {
    Em<caret>pty
}
