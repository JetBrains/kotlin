// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -NOTHING_TO_INLINE
// LANGUAGE: -ForbidExposingLessVisibleTypesInInline

private fun f() {}
private val p = 1

private class PC {
    fun f() {}
    val p = 1
}

internal inline fun test() {
    ::<!CALLABLE_REFERENCE_TO_LESS_VISIBLE_DECLARATION_IN_INLINE_WARNING!>f<!>
    ::<!CALLABLE_REFERENCE_TO_LESS_VISIBLE_DECLARATION_IN_INLINE_WARNING!>p<!>

    PC::<!LESS_VISIBLE_CONTAINING_CLASS_IN_INLINE_WARNING, PRIVATE_CLASS_MEMBER_FROM_INLINE!>f<!>
    PC::<!LESS_VISIBLE_CONTAINING_CLASS_IN_INLINE_WARNING, PRIVATE_CLASS_MEMBER_FROM_INLINE!>p<!>

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
        ::<!CALLABLE_REFERENCE_TO_LESS_VISIBLE_DECLARATION_IN_INLINE_WARNING!>f<!>
        ::<!CALLABLE_REFERENCE_TO_LESS_VISIBLE_DECLARATION_IN_INLINE_WARNING!>p<!>
        ::<!CALLABLE_REFERENCE_TO_LESS_VISIBLE_DECLARATION_IN_INLINE_WARNING!>protectedF<!>
        ::<!CALLABLE_REFERENCE_TO_LESS_VISIBLE_DECLARATION_IN_INLINE_WARNING!>protectedP<!>

        PC::<!CALLABLE_REFERENCE_TO_LESS_VISIBLE_DECLARATION_IN_INLINE_WARNING, LESS_VISIBLE_CONTAINING_CLASS_IN_INLINE_WARNING!>f<!>
        PC::<!CALLABLE_REFERENCE_TO_LESS_VISIBLE_DECLARATION_IN_INLINE_WARNING, LESS_VISIBLE_CONTAINING_CLASS_IN_INLINE_WARNING!>p<!>
    }
}
