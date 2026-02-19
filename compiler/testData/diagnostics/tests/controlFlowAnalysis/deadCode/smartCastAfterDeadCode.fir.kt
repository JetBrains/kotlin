// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// ISSUE: KT-41728

fun foo(a: String?) {
    if (a == null) {
        return
        <!UNREACHABLE_CODE!>"hi".length<!>
    }
    a.length
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, ifExpression, nullableType, smartcast, stringLiteral */
