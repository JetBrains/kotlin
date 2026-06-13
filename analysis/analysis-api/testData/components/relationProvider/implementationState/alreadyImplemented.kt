interface Base {
    fun foo()
    val bar: Int
}

class <caret>Child : Base {
    override fun foo() {}
    override val bar: Int get() = 42
}
