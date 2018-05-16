class TEST {
    companion object {
        fun foo(i: Int, s: String) {}
    }
    fun bar(i: Int, s: String) = <caret>Companion.foo(i, s)
}