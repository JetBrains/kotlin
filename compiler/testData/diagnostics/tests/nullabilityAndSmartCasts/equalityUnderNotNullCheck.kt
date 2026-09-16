// RUN_PIPELINE_TILL: CODEGEN
fun test(a: Any?) {
    if (a is String) {
        a == ""
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, ifExpression, isExpression, nullableType, smartcast,
stringLiteral */
