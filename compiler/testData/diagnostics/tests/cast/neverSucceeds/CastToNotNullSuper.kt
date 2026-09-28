// RUN_PIPELINE_TILL: CODEGEN
open class A {
    fun foo() {}
}
class B : A()

fun test(b: B?) {
    (b as A).foo()
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, functionDeclaration, nullableType */
