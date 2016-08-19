enum class EE(val x: Int) {
    INSTANCE(Companion.foo()),
    ANOTHER(foo());

    companion object {
        fun foo() = 42
    }
}