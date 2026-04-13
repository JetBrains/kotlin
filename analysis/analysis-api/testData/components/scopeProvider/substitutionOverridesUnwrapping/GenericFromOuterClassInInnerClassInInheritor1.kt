package test

class SomeClass

open class TopLevel<Outer> {
    open inner class Base {
        fun noGeneric() {}
        fun withOuter(): Outer? = null
    }
}

class OtherTopLevel : TopLevel<SomeClass>() {
    inner class <caret>Child : Base()
}
