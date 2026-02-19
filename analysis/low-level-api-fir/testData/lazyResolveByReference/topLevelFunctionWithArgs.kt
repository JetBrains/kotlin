// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
package library

fun topLevelFunction() {}

fun topLevelFunction(s: String) {}

// MODULE: main(lib)
// FILE: main.kt
package test

import library.topLevelFunction

fun usage() {
    <caret>topLevelFunction("")
}