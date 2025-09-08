// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
class A {
    fun test() {
        val a: A
        synchronized(this) {
            if (bar()) throw RuntimeException()
            a = A()
        }
        a.bar()
    }

    fun bar() = false
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, ifExpression, lambdaLiteral, localProperty,
propertyDeclaration, thisExpression */
