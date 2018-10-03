// PROBLEM: none

interface Foo {
    fun test()
}

interface Gav {
    fun test() {}
}

class TwoInterfaces : Foo, Gav {
    override <caret>fun test() = super.test()
}