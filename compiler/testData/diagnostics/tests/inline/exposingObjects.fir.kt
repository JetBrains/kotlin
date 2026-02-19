// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -NOTHING_TO_INLINE
// LANGUAGE: -ForbidExposingLessVisibleTypesInInline

class C {
    private companion object Companion
    private object Obj
    typealias <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>TA<!> = Companion

    inline fun foo() {
        <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING, NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>Companion<!>
        <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>Obj<!>
        <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>TA<!>
    }

    internal inline fun foo2() {
        <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>Companion<!>
        <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>Obj<!>
        <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>TA<!>
    }
}

class C2 {
    protected companion object Companion
    protected object Obj
    typealias <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>TA<!> = Companion

    inline fun foo() {
        <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>Companion<!>
        <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>Obj<!>
        <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>TA<!>
    }

    internal inline fun foo2() {
        <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>Companion<!>
        <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>Obj<!>
        <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>TA<!>
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, inline, nestedClass, objectDeclaration,
typeAliasDeclaration */
