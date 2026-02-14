// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// NI_EXPECTED_FILE

fun foo(first: Array<Any?>, second: Array<Any?>) = Pair(<!NO_VALUE_FOR_PARAMETER!>first.toCollection()<!>, <!NO_VALUE_FOR_PARAMETER!>second.toCollection()<!>)

/* GENERATED_FIR_TAGS: functionDeclaration, inProjection, nullableType */
