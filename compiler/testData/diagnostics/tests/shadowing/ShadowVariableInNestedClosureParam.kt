// RUN_PIPELINE_TILL: BACKEND
fun ff(): Int {
    var i = 1
    { i: Int -> i }
    return i
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, lambdaLiteral, localProperty, propertyDeclaration */
