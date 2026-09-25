// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP
fun interface Sam {
    fun invoke()
}

<!WRONG_MODIFIER_TARGET!>error<!> class Foo

fun foo(f: Sam | Foo) { }

fun test() {
    foo {}
}

/* GENERATED_FIR_TAGS: classDeclaration, funInterface, functionDeclaration, interfaceDeclaration, lambdaLiteral,
samConversion */
