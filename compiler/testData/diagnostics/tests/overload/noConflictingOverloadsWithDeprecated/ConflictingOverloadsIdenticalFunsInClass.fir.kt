class A() {
    fun b() {
    }

    @Deprecated("b", level = DeprecationLevel.HIDDEN)
    fun b() {
    }
}
