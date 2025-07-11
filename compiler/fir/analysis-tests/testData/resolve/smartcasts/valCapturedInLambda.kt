// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-43846

fun test_1(x: Any): String {
    if (x is String) {
        val thunk = { x }
        return thunk()
    }
    return "str"
}

fun test_2(x: Any): String {
    if (x is String) {
        val thunk = { x + "a" }
        return thunk()
    }
    return "str"
}

/* GENERATED_FIR_TAGS: additiveExpression, functionDeclaration, ifExpression, isExpression, lambdaLiteral, localProperty,
propertyDeclaration, smartcast, stringLiteral */
