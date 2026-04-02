// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_PARAMETER
fun foo(arg: Int?) {
    run {
        var x = arg
        while (x != null) {
            x = x.hashCode()
            if (x == 0) x = null
        }
    }
}

/* GENERATED_FIR_TAGS: assignment, equalityExpression, functionDeclaration, ifExpression, integerLiteral, lambdaLiteral,
localProperty, nullableType, propertyDeclaration, smartcast, whileLoop */
