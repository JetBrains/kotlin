// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL
class A {
    companion object B {
        class <!REDECLARATION!>G<!>
        val <!REDECLARATION!>G<!> = 1
    }
}