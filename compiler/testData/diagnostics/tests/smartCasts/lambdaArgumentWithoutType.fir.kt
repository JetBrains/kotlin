// RUN_PIPELINE_TILL: BACKEND
// See KT-5385: no smart cast in a literal without given type arguments

interface Foo
fun foo(): Foo? = null

val foo: Foo = run {
    val x = foo()
    if (x == null) throw Exception()
    x
}

// Basic non-lambda case

fun <T> repeat(arg: T): T = arg

fun bar(): Foo {
    val x = foo()
    if (x == null) throw Exception()
    return repeat(x)
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, ifExpression, interfaceDeclaration, lambdaLiteral,
localProperty, nullableType, propertyDeclaration, smartcast, typeParameter */
