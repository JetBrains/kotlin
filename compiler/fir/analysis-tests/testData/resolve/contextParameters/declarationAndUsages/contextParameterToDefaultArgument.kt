// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters
class C(var a: String) {
    fun foo(): String { return a }
}

context(a: C)
fun test(b: C = a) {
    b.foo()
}

context(a: C)
fun test2(vararg b: C = arrayOf(a)) {
    for (t in b) t.foo()
}

context(a: Array<C>)
fun test3(b: Array<C> = arrayOf(*a)) {
    for (t in b) t.foo()
}

fun usage(){
    with(C("OK")) {
        test()
        test2()
    }
    with(arrayOf(C("OK"))) {
        test3()
    }
}

/* GENERATED_FIR_TAGS: capturedType, classDeclaration, forLoop, functionDeclaration, functionDeclarationWithContext,
lambdaLiteral, localProperty, outProjection, primaryConstructor, propertyDeclaration, stringLiteral, vararg */
