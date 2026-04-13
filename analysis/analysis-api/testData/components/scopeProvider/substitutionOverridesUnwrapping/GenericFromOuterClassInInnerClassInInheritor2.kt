package test

class SomeClass

open class TopLevel<Outer> {
    open inner class Base {
        fun noGeneric() {}
        fun withOuter(): Outer? = null
    }
}

class OtherTopLevel<T> : TopLevel<T>() {
    inner class <caret>Child : Base()
}
