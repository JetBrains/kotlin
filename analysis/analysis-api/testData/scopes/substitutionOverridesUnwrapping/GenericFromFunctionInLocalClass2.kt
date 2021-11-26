// DO_NOT_CHECK_SYMBOL_RESTORE
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
