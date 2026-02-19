// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
package library

fun topLevelFunction() {}

fun topLevelFunction(s: String) {}

val topLevelProperty: Int = 0

// MODULE: main(lib)
// FILE: main.kt
package test

import library.topLevelProperty

fun usage() {
    val a = <caret>topLevelProperty
}