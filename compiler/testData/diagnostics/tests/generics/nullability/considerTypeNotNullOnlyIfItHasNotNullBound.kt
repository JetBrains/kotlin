// RUN_PIPELINE_TILL: FRONTEND

inline fun <T, reified S> foo(x: T?, y: T): T {
    if (x is S) return <!RETURN_TYPE_MISMATCH!>x<!>
    return y
}

/* GENERATED_FIR_TAGS: functionDeclaration, ifExpression, inline, intersectionType, isExpression, nullableType, reified,
smartcast, typeParameter */
