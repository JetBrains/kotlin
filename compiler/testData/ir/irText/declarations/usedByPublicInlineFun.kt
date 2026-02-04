// ISSUE: KT-81432
// FIR_IDENTICAL
class Foo {
    protected fun protectedFun() = Unit
    internal fun internalFun() = Unit

    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE", "PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR")
    inline fun publicInline() {
        protectedFun()
        internalFun()
    }
}

open class OpenFoo {
    protected fun protectedFun() = Unit
    internal fun internalFun() = Unit

    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE", "PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR")
    inline fun publicInline() {
        protectedFun()
        internalFun()
    }
}
