// FIR_IDENTICAL
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K1: JVM_IR
// ISSUE: KT-59038

// FILE: C.java
abstract class C extends B {
    protected C(A a) {
        super(a);
    }
}

// FILE: test.kt
private class D(a: A) : C(a)

open class A

open class B(private val a: A) {
    open fun <T : A> getA(): T {
        return a as T
    }
}
