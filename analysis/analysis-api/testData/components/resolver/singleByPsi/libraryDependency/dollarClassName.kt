// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: Bar.kt
package library

class `Outer$Bar` {
    fun foo(a: Int, b: String) {}
}

// MODULE: main(library)
// FILE: main.kt
package test

import library.`Outer$Bar`

fun call() {
    val bar = `Outer$Bar`()
    bar.<expr>foo(1, "foo")</expr>
}
