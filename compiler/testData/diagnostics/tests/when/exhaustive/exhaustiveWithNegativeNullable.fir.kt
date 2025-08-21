// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +DataFlowBasedExhaustiveness

fun foo(b: Boolean?): Int {
    if (b == null) return 1
    return when (b) {
        true -> 2
        false -> 3
    }
}

fun bar(b: Boolean?): Int {
    if (b != null) return 1
    return when (b) {
        <!SENSELESS_COMPARISON!>null<!> -> 2
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, ifExpression, integerLiteral, nullableType, smartcast,
whenExpression, whenWithSubject */
