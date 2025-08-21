// RUN_PIPELINE_TILL: FRONTEND
fun test1() {
    try {
        { <!CANNOT_INFER_PARAMETER_TYPE!>toDouble<!> ->
        }
    } catch (e: Exception) {

    }
}

fun test2() {
    try {

    } catch (e: Exception) {
        { <!CANNOT_INFER_PARAMETER_TYPE!>toDouble<!> ->
        }
    }
}

fun box(): String {
    test1()
    test2()
    return "OK"
}

/* GENERATED_FIR_TAGS: functionDeclaration, lambdaLiteral, localProperty, propertyDeclaration, stringLiteral,
tryExpression */
