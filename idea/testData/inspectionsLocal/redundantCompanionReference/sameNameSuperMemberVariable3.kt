// PROBLEM: none

interface A {
    val foo: String
        get() = "A"
}

open class B {
}

class C : B(), A {
    fun test(): String {
        return <caret>Companion.foo
    }

    companion object {
        val foo = "C"
    }
}
