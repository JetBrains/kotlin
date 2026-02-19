// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -NOTHING_TO_INLINE
// LANGUAGE: +ForbidExposingLessVisibleTypesInInline

private fun f() {}
private val p = 1

private class PC {
    fun f() {}
    val p = 1
}

class A private constructor() {
    public inline fun publicInlineFunction() = ::<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>A<!>
}

private fun Int.privateExtensionFun() {}

private val String.privateVal: String
    get() = this

inline fun test() {
    ::<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>f<!>
    ::<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>p<!>

    <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_ERROR!>PC<!>::<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>f<!>
    <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_ERROR!>PC<!>::<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>p<!>

    val o = object {
        private fun f() {}
        private val p = 1

        fun inAnonymousObject() {
            ::f
            ::p
        }
    }

    Int::<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>privateExtensionFun<!>
    String::<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>privateVal<!>
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

        <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_ERROR!>PC<!>::<!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>f<!>
        <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_ERROR!>PC<!>::<!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>p<!>
    }

    @PublishedApi
    internal inline fun test2() {
        ::<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>f<!>
        ::<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>p<!>
        ::<!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>protectedF<!>
        ::<!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>protectedP<!>

        <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_ERROR!>PC<!>::<!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>f<!>
        <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_ERROR!>PC<!>::<!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>p<!>
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, callableReference, classDeclaration, functionDeclaration, inline,
integerLiteral, localProperty, nestedClass, propertyDeclaration */
