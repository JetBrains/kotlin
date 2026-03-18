// DO_NOT_REQUIRE_SYMBOL_RESTORATION_K1
package test

class SomeClass

fun <Outer> topLevel() {
    open class Base {
        fun withOuter(): Outer? = null
    }

    class <caret>Child : Base() {
        fun noGenerics() {}
    }
}
