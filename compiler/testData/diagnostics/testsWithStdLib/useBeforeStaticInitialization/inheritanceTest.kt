// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL

abstract class A {
    abstract fun foo(): Unit
}

open class B : A() {
    override fun foo() {}
}

object C : A() {
    override fun foo() {}
}

class D : B()

class E : B() {
    override fun foo() {}
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, objectDeclaration, override */
