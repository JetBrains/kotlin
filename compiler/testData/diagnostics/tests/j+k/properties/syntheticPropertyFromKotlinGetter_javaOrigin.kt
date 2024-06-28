// FIR_IDENTICAL
// ISSUE: KT-63076

// FILE: A.java
public class A {
    public String getX1() { return ""; }
    public String getX2() { return ""; }
    public String getX3() { return ""; }
    public String getX4() { return ""; }
}

// FILE: B.kt
open class B : A() {
    override fun getX2(): String = ""
    override fun getX3(): String = ""
    override fun getX4(): String = ""
}

// FILE: C.java
public class C extends B {
    public String getX3() { return ""; }
    public String getX4() { return ""; }
}

// FILE: D.kt
open class D : C() {
    override fun getX4(): String = ""
}

// FILE: main.kt

fun test_1(a: A) {
    a.x1
    a.x2
    a.x3
    a.x4
}

fun test_2(b: B) {
    b.x1
    b.x2
    b.x3
    b.x4
}

fun test_3(c: C) {
    c.x1
    c.x2
    c.x3
    c.x4
}

fun test_4(d: D) {
    d.x1
    d.x2
    d.x3
    d.x4
}
