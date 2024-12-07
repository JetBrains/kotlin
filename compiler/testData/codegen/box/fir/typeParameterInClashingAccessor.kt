// TARGET_BACKEND: JVM_IR
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

class E : A()

open class B(private val a: A) {
    open fun <T : A> getA(): T {
        return a as T
    }
}

fun box(): String {
    val a = A()
    val da = D(a)
    val aa = da.getA<A>()
    if (a !== aa) return "FAIL 1"

    val e = E()
    val de = D(e)
    val ee = de.getA<E>()
    if (e !== ee) return "FAIL 2"

    return "OK"
}
