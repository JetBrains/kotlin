// RUN_PIPELINE_TILL: CODEGEN
interface A {
    fun foo()
}

interface C: A
interface B: A

fun test(c: C) {
    if (c is B) {
        c.foo() // OVERLOAD_RESOLUTION_AMBIGUITY: B.foo() and C.foo()
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, ifExpression, interfaceDeclaration, isExpression */
