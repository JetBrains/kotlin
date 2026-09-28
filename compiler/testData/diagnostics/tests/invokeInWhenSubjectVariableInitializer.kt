// RUN_PIPELINE_TILL: CODEGEN
fun test(func: () -> String?) {
    val x = func() ?: ""
}

/* GENERATED_FIR_TAGS: elvisExpression, functionDeclaration, functionalType, localProperty, nullableType,
propertyDeclaration, stringLiteral */
