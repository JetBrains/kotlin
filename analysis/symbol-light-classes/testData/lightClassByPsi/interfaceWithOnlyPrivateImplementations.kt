interface OnlyPrivateImplementations {
    private fun foo() {}

    private val bar: Int
        get() = 0

    fun baz()
}
