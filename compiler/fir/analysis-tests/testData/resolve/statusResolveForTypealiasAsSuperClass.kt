// RUN_PIPELINE_TILL: BACKEND
class A : Foo {
    override fun foo() {}
}

typealias Foo = B

interface B {
    fun foo() {}
}

fun test(c: A) {
    c.foo()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, override, typeAliasDeclaration */
