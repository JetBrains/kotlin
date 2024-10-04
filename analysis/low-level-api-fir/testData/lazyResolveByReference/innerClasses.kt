// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
package library

class WithInner {
    inner class Inner {}

    fun foo(): Inner = Inner()
}

// MODULE: main(lib)
// FILE: main.kt
package test

import library.WithInner

fun usage() {
    WithInner().f<caret>oo()
}