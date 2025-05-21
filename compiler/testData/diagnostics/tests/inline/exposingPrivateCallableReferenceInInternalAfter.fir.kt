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
    ::<!CALLABLE_REFERENCE_TO_LESS_VISIBLE_DECLARATION_IN_INLINE_ERROR!>f<!>
    ::<!CALLABLE_REFERENCE_TO_LESS_VISIBLE_DECLARATION_IN_INLINE_ERROR!>p<!>

    <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_ERROR!>PC<!>::<!LESS_VISIBLE_CONTAINING_CLASS_IN_INLINE_ERROR, PRIVATE_CLASS_MEMBER_FROM_INLINE!>f<!>
    <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_ERROR!>PC<!>::<!LESS_VISIBLE_CONTAINING_CLASS_IN_INLINE_ERROR, PRIVATE_CLASS_MEMBER_FROM_INLINE!>p<!>

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
        ::<!CALLABLE_REFERENCE_TO_LESS_VISIBLE_DECLARATION_IN_INLINE_ERROR!>f<!>
        ::<!CALLABLE_REFERENCE_TO_LESS_VISIBLE_DECLARATION_IN_INLINE_ERROR!>p<!>
        ::<!CALLABLE_REFERENCE_TO_LESS_VISIBLE_DECLARATION_IN_INLINE_ERROR!>protectedF<!>
        ::<!CALLABLE_REFERENCE_TO_LESS_VISIBLE_DECLARATION_IN_INLINE_ERROR!>protectedP<!>

        <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_ERROR!>PC<!>::<!CALLABLE_REFERENCE_TO_LESS_VISIBLE_DECLARATION_IN_INLINE_ERROR, LESS_VISIBLE_CONTAINING_CLASS_IN_INLINE_ERROR!>f<!>
        <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_ERROR!>PC<!>::<!CALLABLE_REFERENCE_TO_LESS_VISIBLE_DECLARATION_IN_INLINE_ERROR, LESS_VISIBLE_CONTAINING_CLASS_IN_INLINE_ERROR!>p<!>
    }
}
