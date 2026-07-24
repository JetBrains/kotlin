// RUN_PIPELINE_TILL: FRONTEND

abstract class A {
    fun foo(b: B) {
        b.<!INVISIBLE_REFERENCE!>prv<!>()
    }

    private fun prv() {}
}

abstract class B : A()

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration */
