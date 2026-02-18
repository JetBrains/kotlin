// IGNORE_BACKEND_K1: JVM_IR

open class Base {
    @Deprecated("", level = DeprecationLevel.HIDDEN)
    @java.lang.Deprecated
    open fun f() {}
}

interface I {
    @Deprecated("", level = DeprecationLevel.HIDDEN)
    @java.lang.Deprecated
    fun f() = 1
}