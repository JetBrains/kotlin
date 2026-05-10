// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: Bar.kt
package library

class `Outer$Bar` {
    fun foo(a: Int, b: String) {}

    class `Nested$Bar` {
        fun nestedFoo(a: Int, b: String) {}
    }
}

// MODULE: main(library)
// FILE: main.kt
package test

import library.`Outer$Bar`

fun call() {
    val nestedBar = `Outer$Bar`.`Nested$Bar`()
    nestedBar.<expr>nestedFoo(1, "foo")</expr>
}
