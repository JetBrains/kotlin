// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KTIJ-15380

fun test() {
    var x = 0
    if (x == x) {}
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, ifExpression, integerLiteral, localProperty,
propertyDeclaration */
