// RUN_PIPELINE_TILL: CODEGEN
// FIR_DUMP
fun interface Sam {
    fun invoke()
}

error class Foo

fun foo(f: Sam | Foo) { }

fun test() {
    foo {}
}

/* GENERATED_FIR_TAGS: classDeclaration, funInterface, functionDeclaration, interfaceDeclaration, lambdaLiteral,
samConversion */
