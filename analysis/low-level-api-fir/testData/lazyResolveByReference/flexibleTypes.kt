// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
package library

class WithFlexibleTypes {
    val str = java.util.Arrays.asList("hello").get(0)
}

// MODULE: main(lib)
// FILE: main.kt
package test

import library.WithFlexibleTypes

fun usage() {
    WithFlexibleTypes().s<caret>tr
}