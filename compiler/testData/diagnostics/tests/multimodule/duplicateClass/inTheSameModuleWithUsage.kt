// FIR_IDENTICAL
// MODULE: m1
// FILE: a.kt

package p

public class A
public class B {
    public val a: A = A()
}

// MODULE: m2(m1)
// FILE: b.kt

package p

class A {
    fun foo() {}
}

fun test() {
    val a: A = B().a
    a.foo()
}