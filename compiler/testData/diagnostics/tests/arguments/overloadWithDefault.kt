// RUN_PIPELINE_TILL: CODEGEN
interface A {
    fun foo(b: Boolean = false): A
    fun foo(block: () -> Boolean): A
}

fun test(a: A) {
    a.foo { true }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, interfaceDeclaration, lambdaLiteral */
