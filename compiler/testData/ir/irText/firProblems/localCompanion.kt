// FIR_IDENTICAL

// NO_SIGNATURE_DUMP
// ^KT-57430

fun main() {
    class Foo {
        @Suppress("WRONG_MODIFIER_CONTAINING_DECLARATION")
        companion object {
            fun bar() {}
        }
    }
    Foo.Companion.bar()
}
