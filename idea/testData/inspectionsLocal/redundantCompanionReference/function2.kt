// PROBLEM: none

class TEST {
    companion object {
        fun foo(i: Int, s: String) {}
    }
    fun foo(i: Int, s: String) = <caret>Companion.foo(i, s)
}