class A {
    fun a(a: Int): Int = 0

    @Deprecated("a", level = DeprecationLevel.HIDDEN)
    fun a(a: Int) {
    }
}
