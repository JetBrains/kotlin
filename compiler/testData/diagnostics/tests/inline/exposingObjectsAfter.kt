// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -NOTHING_TO_INLINE
// LANGUAGE: +ForbidExposingLessVisibleTypesInInline

class C {
    private companion object Companion
    private object Obj
    typealias TA = Companion

    inline fun foo() {
        <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>Companion<!>
        <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>Obj<!>
        TA
    }

    internal inline fun foo2() {
        Companion
        Obj
        TA
    }
}

class C2 {
    protected companion object Companion
    protected object Obj
    typealias TA = Companion

    inline fun foo() {
        <!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>Companion<!>
        <!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>Obj<!>
        TA
    }

    internal inline fun foo2() {
        Companion
        Obj
        TA
    }
}
