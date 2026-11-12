// DO_NOT_REQUIRE_SYMBOL_RESTORATION
// DO_NOT_REQUIRE_NON_PSI_SYMBOL_RESTORATION

package test

class Outer {
    fun interface Nested {
        fun f()
    }
}

fun usage() {
    Outer.<caret>Nested { }
}
