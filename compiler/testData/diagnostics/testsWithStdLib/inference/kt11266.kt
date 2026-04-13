// RUN_PIPELINE_TILL: FRONTEND
// NI_EXPECTED_FILE

fun foo(first: Array<Any?>, second: Array<Any?>) = Pair(first.toCollection<!NO_VALUE_FOR_PARAMETER!>()<!>, second.toCollection<!NO_VALUE_FOR_PARAMETER!>()<!>)

/* GENERATED_FIR_TAGS: functionDeclaration, inProjection, nullableType */
