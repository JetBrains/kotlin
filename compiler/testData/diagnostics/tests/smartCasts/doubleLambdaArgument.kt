// RUN_PIPELINE_TILL: BACKEND
interface Foo
fun foo(): Foo? = null

val foo: Foo = run {
    run {
        val x = foo()
        if (x == null) throw Exception()
        <!DEBUG_INFO_SMARTCAST!>x<!>
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, ifExpression, interfaceDeclaration, lambdaLiteral,
localProperty, nullableType, propertyDeclaration, smartcast */
