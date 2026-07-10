// RUN_PIPELINE_TILL: FRONTEND
// DUMP_INFERENCE_LOGS: MARKDOWN
// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_ANONYMOUS_PARAMETER

fun <K> select(x: K, y: K): K = x

fun test_1() {
    select(
        { 1 },
        { "" }
    )
}

/* GENERATED_FIR_TAGS: callableReference, functionDeclaration, functionalType, integerLiteral, interfaceDeclaration,
intersectionType, lambdaLiteral, localProperty, nullableType, propertyDeclaration, stringLiteral, typeParameter */
