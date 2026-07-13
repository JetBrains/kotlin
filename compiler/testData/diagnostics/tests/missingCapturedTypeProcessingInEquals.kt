// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-87253

fun foo(vararg ts: String, a: Any?) {
    if (ts[0] == a) {
        a.length
    }
}

fun <T: <!FINAL_UPPER_BOUND!>String<!>> bar(ts: T, a: Any?) {
    if (ts == a) {
        a.length
    }
}

/* GENERATED_FIR_TAGS: capturedType, equalityExpression, functionDeclaration, ifExpression, integerLiteral, nullableType,
outProjection, smartcast, vararg */
