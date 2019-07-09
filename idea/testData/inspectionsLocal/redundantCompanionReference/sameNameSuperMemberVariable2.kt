// PROBLEM: none

open class A {
    val foo = "A"
}

open class B: A() {
}

class C : B() {
    fun test(): String {
        return <caret>Companion.foo
    }

    companion object {
        val foo = "C"
    }
}
