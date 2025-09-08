// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters

fun test(a: (context(String) () -> Unit)?) {
    if (a != null) {
        a("")
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, functionalType, ifExpression, nullableType, smartcast,
stringLiteral, typeWithContext */
