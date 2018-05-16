class TEST {
    companion object {
        fun foo(i: Int, s: String) {}
    }
    fun foo(i: Int) = <caret>Companion.foo(i, "")
}