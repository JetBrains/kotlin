// PROBLEM: none

open class B {
    val foo = "B"
}

class C : B() {
    fun test(): String {
        return <caret>Companion.foo
    }

    companion object {
        val foo = "C"
    }
}
