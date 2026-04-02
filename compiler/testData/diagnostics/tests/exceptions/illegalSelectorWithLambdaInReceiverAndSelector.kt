// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-72335
// FIR_DUMP

fun bar(b: Boolean, i: Int, block: (Int.() -> Unit)) {
    block({ if (b) "s3" else "s4" }.<!ARGUMENT_TYPE_MISMATCH, ILLEGAL_SELECTOR!>{ i }<!>)
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, ifExpression, lambdaLiteral, stringLiteral,
typeWithExtension */
