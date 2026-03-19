// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-44008
// WITH_STDLIB
// RENDER_DIAGNOSTICS_FULL_TEXT

// KT-44008: Improve diagnostic messages with `TypeVariable(T)` in new inference
fun f(s: List<String>) {
    s.<!CANNOT_INFER_PARAMETER_TYPE!>flatMap<!> { <!RETURN_TYPE_MISMATCH!>it<!> }
}

/* GENERATED_FIR_TAGS: functionDeclaration, lambdaLiteral */
