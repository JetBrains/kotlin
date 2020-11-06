enum class A {
    X, Y;
    companion object {
        fun foo(): Int {
            return 1
        }
    }

    fun foo(): Int = Companion.foo()
}
