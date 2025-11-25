// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-81763
// DIAGNOSTICS: -ERROR_SUPPRESSION
// FIR_DUMP
// DUMP_INFERENCE_LOGS: FIXATION

fun fooElvis(x: Long, y: Long?): Boolean =
    // K of elvis should be inferred to Long, not to Any?
    x != (y ?: 0)

fun fooBar(x: Long, y: Long?): Boolean =
    x != bar(y, 0)

fun <T> bar(x: T?, y: T): T = x ?: y

fun fooBarExact(x: Long, y: Long?): Boolean =
    x != barExact(y, 0)

@Suppress("INVISIBLE_REFERENCE")
fun <E> barExact(x: E?, y: E): @kotlin.internal.Exact E = x ?: y

/* GENERATED_FIR_TAGS: elvisExpression, equalityExpression, functionDeclaration, integerLiteral, nullableType,
typeConstraint, typeParameter */
