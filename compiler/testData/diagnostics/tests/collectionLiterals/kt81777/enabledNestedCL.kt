// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76150
// LANGUAGE: +CompanionBlocks

fun test() {
    <!CANNOT_INFER_PARAMETER_TYPE!>[<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>]<!>
    [["2"]]
    [{}, <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>]
    [::test, [2]]
    [42, <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>]
}

/* GENERATED_FIR_TAGS: callableReference, collectionLiteral, functionDeclaration, integerLiteral, lambdaLiteral,
stringLiteral */
