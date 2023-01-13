// FIR_IDENTICAL
fun main() {
    class Foo {
        @Suppress("WRONG_MODIFIER_CONTAINING_DECLARATION")
        companion object {
            fun bar() {}
        }
    }
    Foo.Companion.bar()
}
