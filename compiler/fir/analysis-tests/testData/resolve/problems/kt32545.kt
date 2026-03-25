// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-32545
// RENDER_DIAGNOSTICS_FULL_TEXT

// KT-32545: TYPE_MISMATCH diagnostic: different order/presentation of errors in compiler and IDE
fun <T> f(it: T): T = <!RETURN_TYPE_MISMATCH!>if (it is String) "" else it<!>

/* GENERATED_FIR_TAGS: functionDeclaration, ifExpression, isExpression, nullableType, smartcast, stringLiteral,
typeParameter */
