// RUN_PIPELINE_TILL: FRONTEND
class A {
    private class AInner
}

class B {
    fun foo(<!EXPOSED_PARAMETER_TYPE!>value: A.<!INVISIBLE_REFERENCE!>AInner<!><!>) {

    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nestedClass */
