// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
package library

enum class TopLevelEnum {
    ENTRY1,
    ENTRY2,
    ENTRY3,
}

// MODULE: main(lib)
// FILE: main.kt
package test

import library.TopLevelEnum

fun usage() {
    val a = TopLevelEnum.<caret>ENTRY1
}
