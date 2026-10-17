// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: +CompanionBlocks

fun test() {
    [1, 2, 3].toString()
    <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>[0]
    val x = [0] <!UNRESOLVED_REFERENCE!>+<!> [1] + [2]
}

/* GENERATED_FIR_TAGS: additiveExpression, functionDeclaration, integerLiteral, localProperty, propertyDeclaration */
