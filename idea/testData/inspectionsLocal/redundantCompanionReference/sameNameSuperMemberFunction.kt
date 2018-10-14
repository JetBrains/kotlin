// PROBLEM: none

open class B {
    fun foo(x: Int) = "B$x"
}

class C : B() {
    fun test(): String {
        return <caret>Companion.foo(1)
    }

    companion object {
        fun foo(x: Int) = "C$x"
    }
}
