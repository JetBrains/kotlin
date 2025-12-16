// FIR_IDENTICAL
// TARGET_BACKEND: JVM

// FILE: A.java
public class A {
    public void foo(String a) {}
}

// FILE: main.kt
interface B {
    fun foo(a: String)
}

interface C {
    fun foo(a: String?)
}

// Any other order of supertypes leads to ABSTRACT_MEMBER_NOT_IMPLEMENTED and/or ACCIDENTAL_OVERRIDE and/or CONFLICTING_JVM_DECLARATIONS
class D : A(), B, C

class E : A(), C, B
