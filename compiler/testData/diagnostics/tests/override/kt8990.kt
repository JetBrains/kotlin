// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
open class A {
    private fun foo() {}

    inner class B : A() {
        private fun foo() {}
    }
}

class C : A() {
    private fun foo() {}
}