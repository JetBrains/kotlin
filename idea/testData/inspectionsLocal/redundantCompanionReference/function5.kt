class TEST {
    companion object {
        fun foo(i: Int, s: String) {}
    }
    fun foo(i: Int, s: String) = TEST.<caret>Companion.foo(i, s)
}