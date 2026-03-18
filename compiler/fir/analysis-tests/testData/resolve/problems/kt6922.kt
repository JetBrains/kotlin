// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-6922

// KT-6922: "x is Int" when type of x is Unit should be a warning, not an error
fun foo(x: Unit) {
    <!IMPOSSIBLE_IS_CHECK_ERROR!>x is Int<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, isExpression */
