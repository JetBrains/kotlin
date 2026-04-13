// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -NOTHING_TO_INLINE
// LANGUAGE: -ForbidExposingLessVisibleTypesInInline

private fun f() {}
private val p = 1

private class PC {
    fun f() {}
    val p = 1
}

class A private constructor() {
    internal inline fun internalInlineFunction() = ::<!CALLABLE_REFERENCE_TO_LESS_VISIBLE_DECLARATION_IN_INLINE_WARNING!>A<!>
}

private fun Int.privateExtensionFun() {}

private val String.privateVal: String
    get() = this

internal inline fun test() {
    ::<!CALLABLE_REFERENCE_TO_LESS_VISIBLE_DECLARATION_IN_INLINE_WARNING!>f<!>
    ::<!CALLABLE_REFERENCE_TO_LESS_VISIBLE_DECLARATION_IN_INLINE_WARNING!>p<!>

    <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>PC<!>::<!PRIVATE_CLASS_MEMBER_FROM_INLINE!>f<!>
    <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>PC<!>::<!PRIVATE_CLASS_MEMBER_FROM_INLINE!>p<!>

    val o = object {
        private fun f() {}
        private val p = 1

        fun inAnonymousObject() {
            ::f
            ::p
        }
    }

    Int::<!CALLABLE_REFERENCE_TO_LESS_VISIBLE_DECLARATION_IN_INLINE_WARNING!>privateExtensionFun<!>
    String::<!CALLABLE_REFERENCE_TO_LESS_VISIBLE_DECLARATION_IN_INLINE_WARNING!>privateVal<!>
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

        <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>PC<!>::<!CALLABLE_REFERENCE_TO_LESS_VISIBLE_DECLARATION_IN_INLINE_WARNING!>f<!>
        <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>PC<!>::<!CALLABLE_REFERENCE_TO_LESS_VISIBLE_DECLARATION_IN_INLINE_WARNING!>p<!>
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, callableReference, classDeclaration, functionDeclaration, inline,
integerLiteral, localProperty, nestedClass, propertyDeclaration */
