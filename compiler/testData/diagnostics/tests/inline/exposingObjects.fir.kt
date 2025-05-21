// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -NOTHING_TO_INLINE
// LANGUAGE: -ForbidExposingLessVisibleTypesInInline

class C {
    private companion object Companion
    private object Obj
    <!UNSUPPORTED_FEATURE!>typealias <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>TA<!> = Companion<!>

    inline fun foo() {
        <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>Companion<!>
        Obj
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
    <!UNSUPPORTED_FEATURE!>typealias <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>TA<!> = Companion<!>

    inline fun foo() {
        Companion
        Obj
        TA
    }

    internal inline fun foo2() {
        Companion
        Obj
        TA
    }
}
