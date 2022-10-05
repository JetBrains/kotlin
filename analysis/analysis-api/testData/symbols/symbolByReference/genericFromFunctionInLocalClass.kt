package test

class SomeClass

fun <Outer> topLevel() {
    open class Base {
        fun withOuter(): Outer? = null
    }

    class Child : Base() {}

    Child().<caret>withOuter()
}