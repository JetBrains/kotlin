// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
package library

annotation class Anno(val name: String)

@Anno("WithFoo")
class WithAnno {}

// MODULE: main(lib)
// FILE: main.kt
package test

import library.WithAnno

fun usage() {
    val w: With<caret>Anno? = null
}
