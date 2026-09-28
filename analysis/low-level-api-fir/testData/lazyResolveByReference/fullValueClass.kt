// LANGUAGE: +FullValueClasses
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
package library

value class Point(val x: Int, val y: Int) {
    val sum: Int get() = x + y
}

// MODULE: main(lib)
// FILE: main.kt
package test

import library.Point

fun usage() {
    val point: Po<caret>int
}
