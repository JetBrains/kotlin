// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -NOTHING_TO_INLINE
// LANGUAGE: -ForbidExposingLessVisibleTypesInInline

class C {
    private companion object Companion
    private object Obj
    <!TOPLEVEL_TYPEALIASES_ONLY!>typealias <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>TA<!> = Companion<!>

    inline fun foo() {
        <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>Companion<!>
        <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>Obj<!>
        <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>TA<!>
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
    <!TOPLEVEL_TYPEALIASES_ONLY!>typealias <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>TA<!> = Companion<!>

    inline fun foo() {
        <!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>Companion<!>
        <!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>Obj<!>
        <!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>TA<!>
    }

    internal inline fun foo2() {
        Companion
        Obj
        TA
    }
}
