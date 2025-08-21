// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ForbidExposingLessVisibleTypesInInline
private class Private

internal <!NOTHING_TO_INLINE!>inline<!> fun test(
    noinline a: (Any) -> Unit = {
        it is Private
        <!PRIVATE_CLASS_MEMBER_FROM_INLINE!>Private<!>()
    },
) {
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, inline, isExpression, lambdaLiteral,
noinline */
