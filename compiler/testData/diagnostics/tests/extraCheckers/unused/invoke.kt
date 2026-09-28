// RUN_PIPELINE_TILL: CODEGEN
fun foo(): Int {
    val x = fun() = 4
    val y = fun() = 2
    return 10 * x() + y()
}

/* GENERATED_FIR_TAGS: additiveExpression, anonymousFunction, functionDeclaration, integerLiteral, localProperty,
multiplicativeExpression, propertyDeclaration */
