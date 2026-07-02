// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-87253

fun foo(vararg ts: String, a: Any?) {
    if (ts[0] == a) {
        a.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

/* GENERATED_FIR_TAGS: capturedType, equalityExpression, functionDeclaration, ifExpression, integerLiteral, nullableType,
outProjection, smartcast, vararg */
