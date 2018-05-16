// PROBLEM: none

class TEST {
    companion object {
        val foo = ""
    }
    val foo: String
        get() = <caret>Companion.foo
}