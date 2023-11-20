// FIR_IDENTICAL

// MUTE_SIGNATURE_COMPARISON_K2: JVM_IR
// ^ KT-57755: Fix computing a mangled name for the `main` function

fun main() {
    class Foo {
        @Suppress("WRONG_MODIFIER_CONTAINING_DECLARATION")
        companion object {
            fun bar() {}
        }
    }
    Foo.Companion.bar()
}
