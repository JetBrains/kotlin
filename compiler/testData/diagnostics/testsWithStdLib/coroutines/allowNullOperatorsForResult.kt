// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_EXPRESSION

fun test(r: Result<Int>?) {
    r ?: 0
    r?.isFailure
}

/* GENERATED_FIR_TAGS: elvisExpression, functionDeclaration, integerLiteral, nullableType, safeCall */
