// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ForbidExposingLessVisibleTypesInInline
private class Private

internal <!NOTHING_TO_INLINE!>inline<!> fun test(
    noinline a: (Any) -> Unit = {
        it is <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_ERROR!>Private<!>
        <!LESS_VISIBLE_CONTAINING_CLASS_IN_INLINE_ERROR, LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_ERROR, PRIVATE_CLASS_MEMBER_FROM_INLINE!>Private<!>()
    },
) {
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, inline, isExpression, lambdaLiteral,
noinline */
