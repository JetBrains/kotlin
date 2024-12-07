package test

class SomeClass

class TopLevel<Outer> {
    inner open class Base<T> {
        fun withOuter(): Outer? = null
    }

    inner class Child : Base<SomeClass> {}

    fun usage() {
        Child().<caret>withOuter()
    }
}
