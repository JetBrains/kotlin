// RUN_PIPELINE_TILL: CODEGEN
// FIR_DUMP
fun interface Sam {
    fun invoke()
}

error class Foo

typealias SamOrFoo = Sam | Foo
typealias S = Sam
typealias SOrFoo = S | Foo

fun foo(f: SamOrFoo) {}
fun bar(f: SOrFoo) {}
fun baz(f: S | Foo) {}

fun test() {
    foo {}
    bar {}
    baz {}
    val f: SamOrFoo = {}
}

/* GENERATED_FIR_TAGS: classDeclaration, funInterface, functionDeclaration, interfaceDeclaration, lambdaLiteral,
localProperty, propertyDeclaration, samConversion, typeAliasDeclaration */
