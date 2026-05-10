// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-23909

// KT-23909: Use more specific diagnostic for wrong calls with implicit invokes - TYPE_MISMATCH instead of FUNCTION_EXPECTED
fun test() {
    1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>(fun String.() = 1)<!>()
}

/* GENERATED_FIR_TAGS: anonymousFunction, functionDeclaration, integerLiteral */
