// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_PARAMETER
fun foo(arg: Int?) = run {
    var x = arg
    if (x == null) return@run
    x.hashCode()
}

class My {
    fun foo(arg: Int?) = run {
        var x = arg
        if (x == null) return@run
        x.hashCode()
    }

    fun Int?.bar() = run {
        var x = this
        if (x == null) return@run
        x.hashCode()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, funWithExtensionReceiver, functionDeclaration, ifExpression,
lambdaLiteral, localProperty, nullableType, propertyDeclaration, smartcast, thisExpression */
