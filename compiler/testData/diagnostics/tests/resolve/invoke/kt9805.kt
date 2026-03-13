// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
class A {
    val foo: B.() -> Unit get() = null!!
}

class B

fun test(a: A, b: B) {
    with(b) {
        <!NO_VALUE_FOR_PARAMETER!>a.foo()<!> // here must be error, because a is not extension receiver

        a.foo(this)

        (a.foo)()

        (a.foo)(this)
    }
}

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, functionDeclaration, functionalType, getter, lambdaLiteral,
propertyDeclaration, thisExpression, typeWithExtension */
