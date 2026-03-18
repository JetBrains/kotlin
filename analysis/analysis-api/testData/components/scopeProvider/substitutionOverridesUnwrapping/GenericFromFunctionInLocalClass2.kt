// DO_NOT_REQUIRE_SYMBOL_RESTORATION_K1
package test

class SomeClass

fun <Outer> topLevel() {
    open class Base<T> {
        fun withOuter(): Outer? = null
        fun withOuterAndOwn(t: T): Outer? = null
    }

    class <caret>Child : Base<SomeClass>() {
        fun noGenerics() {}
    }
}
