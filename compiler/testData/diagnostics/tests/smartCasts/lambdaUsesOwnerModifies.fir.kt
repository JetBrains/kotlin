// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_PARAMETER
fun foo(arg: Int?) {
    var x = arg
    if (x == null) return
    run {
        // Safe: since `run` is in-place
        x.hashCode()
    }
    x = null  
}

/* GENERATED_FIR_TAGS: assignment, equalityExpression, functionDeclaration, ifExpression, lambdaLiteral, localProperty,
nullableType, propertyDeclaration, smartcast */
