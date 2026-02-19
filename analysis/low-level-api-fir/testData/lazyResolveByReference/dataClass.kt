// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
package library

data class MyDataClass(val c: Int) {
    val a: String get() = ""
}

// MODULE: main(lib)
// FILE: main.kt
package test

import library.MyDataClass

fun usage() {
    val t: MyDataC<caret>lass
}