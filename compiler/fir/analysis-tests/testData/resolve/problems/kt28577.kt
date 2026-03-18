// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-28577
// WITH_STDLIB

// KT-28577: Type inference fails to infer return type of list of anonymous objects
fun test1() = listOf(object {})

fun test2(): List<Any> = listOf(object {})

fun test3() = object {}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, functionDeclaration */
