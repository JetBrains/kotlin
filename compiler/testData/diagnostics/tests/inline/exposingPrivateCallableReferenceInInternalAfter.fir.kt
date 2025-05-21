// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -NOTHING_TO_INLINE
// LANGUAGE: +ForbidExposingLessVisibleTypesInInline

private fun f() {}
private val p = 1

private class PC {
    fun f() {}
    val p = 1
}

internal inline fun test() {
    ::f
    ::p

    PC::<!LESS_VISIBLE_CONTAINING_CLASS_IN_INLINE_ERROR, PRIVATE_CLASS_MEMBER_FROM_INLINE!>f<!>
    PC::<!LESS_VISIBLE_CONTAINING_CLASS_IN_INLINE_ERROR, PRIVATE_CLASS_MEMBER_FROM_INLINE!>p<!>

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

    internal inline fun test() {
        ::f
        ::p
        ::protectedF
        ::protectedP

        PC::<!LESS_VISIBLE_CONTAINING_CLASS_IN_INLINE_ERROR!>f<!>
        PC::<!LESS_VISIBLE_CONTAINING_CLASS_IN_INLINE_ERROR!>p<!>
    }
}
