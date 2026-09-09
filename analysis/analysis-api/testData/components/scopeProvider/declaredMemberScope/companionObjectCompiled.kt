// ISSUE: KT-64686
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
package one

class Foo {
    companion object {
        val x: Int = 1

        fun foo() {}
    }
}

// MODULE: main(lib)
// FILE: main.kt

// class: one/Foo.Companion
