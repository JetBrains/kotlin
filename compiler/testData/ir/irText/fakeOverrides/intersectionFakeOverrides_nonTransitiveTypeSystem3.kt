// FIR_IDENTICAL

// FILE: A.java
public class A {
    public void foo(String a) {}
}

// FILE: main.kt
interface B {
    fun foo(a: String) {}
}

interface C {
    fun foo(a: String?)
}

// Any other order of supertypes leads to ABSTRACT_MEMBER_NOT_IMPLEMENTED and/or ACCIDENTAL_OVERRIDE and/or CONFLICTING_JVM_DECLARATIONS
class D : C, B, A() // Duplicated foo's: foo(String?), foo(String)

class E : C, A(), B // Duplicated foo's: foo(String?), foo(String)
