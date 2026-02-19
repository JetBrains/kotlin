// RUN_PIPELINE_TILL: BACKEND
// FIR_DUMP
// LANGUAGE: +ContextParameters +ExplicitContextArguments

open class A
class SubA : A()

context(a: A) fun foo0() { }
fun foo0(a: A = A(), x: Int = 1) { }

context(a: A) fun foo1() { }
fun foo1(a: SubA = SubA(), x: Int = 1) { }

fun test() {
    foo0()
    foo0(a = A())
    foo0(a = A(), x = 2)

    foo1()
    foo1(a = A())
    foo1(a = SubA())
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionDeclarationWithContext, integerLiteral */
