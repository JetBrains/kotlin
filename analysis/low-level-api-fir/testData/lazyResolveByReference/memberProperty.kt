// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
package library

class TopLevelClass() {

    constructor(s: String) : this() {}

    fun memberFunction() {}

    fun memberFunction(s: String) {}

    val memberProperty: Int = 0
}

// MODULE: main(lib)
// FILE: main.kt
package test

import library.TopLevelClass

fun usage() {
    val a = TopLevelClass().<caret>memberProperty
}