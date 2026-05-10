// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-4597
// WITH_STDLIB

// KT-4597: Provide better diagnostics for is-check which never succeeds
fun test() {
    val temp = "abc"
    if (<!IMPOSSIBLE_IS_CHECK_ERROR!>temp is Int<!>) println(temp)
}

/* GENERATED_FIR_TAGS: functionDeclaration, ifExpression, intersectionType, isExpression, localProperty,
propertyDeclaration, smartcast, stringLiteral */
