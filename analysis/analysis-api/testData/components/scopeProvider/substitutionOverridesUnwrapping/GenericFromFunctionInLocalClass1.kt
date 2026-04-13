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
