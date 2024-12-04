// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
package library

object TopLevelObject {
    fun objectFunction() {}
}

// MODULE: main(lib)
// FILE: main.kt
package test

import library.TopLevelObject

fun usage() {
    TopLe<caret>velObject
}