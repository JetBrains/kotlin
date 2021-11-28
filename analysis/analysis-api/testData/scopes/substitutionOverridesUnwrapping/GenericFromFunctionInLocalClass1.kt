// DO_NOT_CHECK_SYMBOL_RESTORE
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
