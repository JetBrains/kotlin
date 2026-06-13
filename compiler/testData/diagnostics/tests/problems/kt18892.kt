// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-18892

// KT-18892: False negative USELESS_CAST for smartcasted variable with safe cast and safe call
fun foo(a: Any) {
    if (a !is CharSequence) return

    (a <!USELESS_CAST!>as CharSequence<!>).length // "No cast needed"
    (a <!USELESS_CAST!>as? CharSequence<!>)?.length // No warning (false negative)
}

/* GENERATED_FIR_TAGS: asExpression, functionDeclaration, ifExpression, isExpression, nullableType, safeCall, smartcast */
