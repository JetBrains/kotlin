// PROBLEM: none

abstract class AbstractClass {
    abstract fun foo(): Int
}

interface Interface {
    fun foo(): Int = 3
}

class ChildClass : AbstractClass(), Interface {
    override <caret>fun foo() = super.foo()
}
