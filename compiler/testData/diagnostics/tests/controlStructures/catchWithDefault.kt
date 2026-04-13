// RUN_PIPELINE_TILL: FRONTEND
fun test() {
    try { } catch (<!CATCH_PARAMETER_WITH_DEFAULT_VALUE!>e: Exception = Exception()<!>) { }
}

/* GENERATED_FIR_TAGS: functionDeclaration, localProperty, propertyDeclaration, tryExpression */
