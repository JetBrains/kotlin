// FIR_IDENTICAL
// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: Java1.java
public class Java1 {
    public int a = 1;
    public void foo(){}
}

// FILE: Java2.java
public class Java2 {
    protected int a = 2;
    protected void foo(){}
}

// FILE: Java3.java
public class Java3 {
    private int a = 3;
    private void foo(){}
}

// FILE: Java4.java
public class Java4 {
    int a = 4;
    void foo(){}
}

// FILE: Java5.java
public class Java5 extends Java1 {
    public int a = 5;
    public void foo(){}
}

// FILE: Java6.java
public class Java6 extends Java2 {
    public int a = 6;
    public void foo() {}
}

// FILE: Java7.java
public class Java7 extends Java3 {
    public int a = 7;
    public void foo() {}
}

// FILE: Java8.java
public class Java8 extends Java4 {
    public int a = 8;
    public void foo(){}
}

// FILE: Java9.java
public class Java9 extends Java3 {
    protected int a = 9;
    protected void foo(){}
}

// FILE: Java10.java
public class Java10 extends Java4 {
    protected int a = 10;
    protected void foo(){}
}

// FILE: Java11.java
public class Java11 extends Java3 {
    int a = 11;
    void foo(){}
}

// FILE: test.kt
class A : Java5()   // (public + public)

class B : Java6()   // (public + protected)

class C : Java7()   // (public + private)

class D : Java8()   // (public + default)

class E : Java9()   // (protected + private)

class F : Java10()  // (protected + default)

class G : Java11()  // (default + private)

class H: Java5() {
    override fun foo() {}
}

class I : Java6() {
    override fun foo() {}
}

class J : Java7() {
    override fun foo() {}
}

class K: Java8() {
    override fun foo() {}
}

class L: Java9() {
    public override fun foo() {}
}

class M : Java10() {
    public override fun foo() {}
}

class N : Java11() {
    internal override fun foo() {}
}

fun test(a:A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N){
    a.a
    a.foo()

    b.a
    b.foo()

    c.a
    c.foo()

    d.a
    d.foo()

    e.a
    e.foo()

    f.a
    f.foo()

    g.a
    g.foo()

    h.a
    h.foo()

    i.a
    i.foo()

    j.a
    j.foo()

    k.a
    k.foo()

    l.a
    l.foo()

    m.a
    m.foo()

    n.a
    n.foo()
}
