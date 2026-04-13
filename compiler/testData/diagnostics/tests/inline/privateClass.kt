// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -EXPOSED_PARAMETER_TYPE
// LANGUAGE: +ForbidExposingLessVisibleTypesInInline

private class S public constructor() {
    fun a() {

    }
}

internal inline fun x(s: <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_ERROR!>S<!>, z: () -> Unit) {
    z()
    <!LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_ERROR, PRIVATE_CLASS_MEMBER_FROM_INLINE!>S<!>()
    <!LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_ERROR!>s<!>.<!LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_ERROR, PRIVATE_CLASS_MEMBER_FROM_INLINE!>a<!>()
    <!LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_ERROR!>test<!>()
}

private inline fun x2(s: S, z: () -> Unit) {
    z()
    S()
    s.a()
    test()
}

private fun test(): S {
    return S()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, inline, primaryConstructor */
