// RUN_PIPELINE_TILL: FRONTEND
fun foo() {
    try {
        throw Exception()
    } catch (<!CATCH_PARAMETER_TYPE_MISMATCH!>x: Nothing<!>) {
    }

    try {
        throw Exception()
    } catch (<!CATCH_PARAMETER_TYPE_MISMATCH!>x: Nothing?<!>) {
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, localProperty, nullableType, propertyDeclaration, tryExpression */
