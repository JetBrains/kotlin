// FIR_IDENTICAL
// TARGET_BACKEND: JVM

// The test primarily tests reflect dumps (k1 vs new reflect), we don't need kt dumps
// SKIP_KT_DUMP

// FILE: A.java
public interface A {
    public void foo(String a);
}

// FILE: main.kt
interface B {
    fun foo(a: String)
}

open class C {
    fun foo(a: String?) {}
}

// Any other order of supertypes leads to ABSTRACT_MEMBER_NOT_IMPLEMENTED and/or ACCIDENTAL_OVERRIDE and/or CONFLICTING_JVM_DECLARATIONS
class D : A, B, C()

class E : A, C(), B
