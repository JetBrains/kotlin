// RUN_PIPELINE_TILL: CODEGEN
// DIAGNOSTICS: -UNUSED_PARAMETER
fun foo(arg: Int?) {
    run {
        var x = arg
        if (x == null) return@run
        x.hashCode()
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, ifExpression, lambdaLiteral, localProperty, nullableType,
propertyDeclaration, smartcast */
