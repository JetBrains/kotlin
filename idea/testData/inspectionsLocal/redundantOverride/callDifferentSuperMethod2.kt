// PROBLEM: none

interface I {
    fun foo(parent: String)
}

open class A {
    open fun foo(parent: Any) {}

}

class B : A(), I {
    override <caret>fun foo(parent: String) {
        super.foo(parent)
    }
}