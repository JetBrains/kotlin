// DO_NOT_REQUIRE_SYMBOL_RESTORATION
// DO_NOT_REQUIRE_NON_PSI_SYMBOL_RESTORATION

// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: Outer.kt
package test

class Outer {
    fun interface Nested {
        fun f()
    }
}

// MODULE: main(library)
// FILE: main.kt
package test

fun usage() {
    Outer.<caret>Nested { }
}
