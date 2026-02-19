// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER
fun foo(arg: Int?) {
    var x = arg
    if (x == null) return
    run {
        // Stable because `run` is in-place
        <!SMARTCAST_IMPOSSIBLE!>x<!>.hashCode()
        x = null
    }
    if (x != null) x = 42
    // Unsafe because of lambda
    <!SMARTCAST_IMPOSSIBLE!>x<!>.hashCode()
}

/* GENERATED_FIR_TAGS: assignment, equalityExpression, functionDeclaration, ifExpression, integerLiteral, lambdaLiteral,
localProperty, nullableType, propertyDeclaration, smartcast */
