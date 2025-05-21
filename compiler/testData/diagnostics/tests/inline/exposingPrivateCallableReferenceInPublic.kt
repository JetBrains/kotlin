// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -NOTHING_TO_INLINE

private fun f() {}
private val p = 1

private class PC {
    fun f() {}
    val p = 1
}

inline fun test() {
    ::<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>f<!>
    ::<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>p<!>

    PC::<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>f<!>
    PC::<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>p<!>

    val o = object {
        private fun f() {}
        private val p = 1

        fun inAnonymousObject() {
            ::f
            ::p
        }
    }
}

class C {
    private fun f() {}
    private val p = 1

    protected fun protectedF() {}
    protected val protectedP = 1

    protected class PC {
        fun f() {}
        val p = 1
    }

    inline fun test() {
        ::<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>f<!>
        ::<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>p<!>
        ::<!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>protectedF<!>
        ::<!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>protectedP<!>

        PC::<!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>f<!>
        PC::<!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>p<!>
    }

    @PublishedApi
    internal inline fun test2() {
        ::<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>f<!>
        ::<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>p<!>
        ::<!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>protectedF<!>
        ::<!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>protectedP<!>

        PC::<!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>f<!>
        PC::<!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>p<!>
    }
}
