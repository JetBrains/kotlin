// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL

fun foo(): Int {
    var result = 0
    try {
    } finally {
        42.let { }
    }
    return result
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, lambdaLiteral, localProperty, propertyDeclaration,
tryExpression */
