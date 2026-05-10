// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_PARAMETER

fun interface Foo {
    fun invoke()
}

fun foo(f: Foo) {}

fun test() {
    foo {}
}

/* GENERATED_FIR_TAGS: funInterface, functionDeclaration, interfaceDeclaration, lambdaLiteral, samConversion */
