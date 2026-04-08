open class Base {
    @Deprecated("", level = DeprecationLevel.HIDDEN)
    open fun f() {}
}

interface I {
    @Deprecated("", level = DeprecationLevel.HIDDEN)
    fun f() = 1
}
