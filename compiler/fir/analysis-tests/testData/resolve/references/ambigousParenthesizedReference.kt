// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-36958

fun consume(x: Any?) {}

fun box() = consume((Int::<!OVERLOAD_RESOLUTION_AMBIGUITY!>plus<!>))

/* GENERATED_FIR_TAGS: functionDeclaration, nullableType */
