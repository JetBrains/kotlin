package test

class SomeClass

open class TopLevel<Outer> {
    open inner class Base {
        fun noGeneric() {}
        fun withOuter(): Outer? = null
    }
    inner class <caret>Child : Base()
}

