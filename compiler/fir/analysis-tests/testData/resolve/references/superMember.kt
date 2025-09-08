// RUN_PIPELINE_TILL: BACKEND
open class A {
    open fun foo() {}
}

class B : A() {
    fun bar() {
        foo()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration */
