// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// MODULE: separate
// FILE: KotlinInternal.kt
open class KotlinInternal {
    internal open val a : Int
        get() = 1
    internal open fun foo(){}
}

// MODULE: main(separate)
// FILE: Java1.java
public class Java1 extends KotlinInternal {}

// FILE: Java2.java
public class Java2 extends KotlinInternal {
    public int a = 7;
    public void foo() {}
}

// FILE: Java3.java
public class Java3 extends KotlinInternal {
    protected int a = 12;
    protected void foo() {}
}

// FILE: Java4.java
public class Java4 extends KotlinInternal {
    private int a = 16;
    private void foo() {}
}


// FILE: Java5.java
public class Java5 extends KotlinInternal {
    int a = 5;
    void foo(){}
}

// FILE: JavaPublic.java
public interface JavaPublic {
    public int a = 2;
    public void foo();
}

// FILE: JavaDefault.java
public interface JavaDefault {
    int a = 2;
    void foo();
}

// FILE: test.kt
class A : Java1()

class B : Java2()

class C: Java2() {
    override fun foo() {}
    val a = 10
}

class D : Java3()

class E : Java3() {
    public override fun foo() {}
    val a = 10
}

class F : Java4()

class G : Java5()

class H : Java5() {
    public override fun foo() {}
    val a = 10
}

abstract class I : JavaPublic, KotlinInternal()

class J : JavaPublic, KotlinInternal() {
    public override fun foo() {}
    val a = 10
}

class K : JavaDefault, KotlinInternal() {
    public override fun foo() {}
    val a = 10
}


fun test(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K){
    b.foo()
    b.a

    c.foo()
    c.a

    d.foo()
    d.a

    e.foo()
    e.a

    g.foo()
    g.a

    h.foo()
    h.a

    i.foo()

    j.foo()
    j.a

    k.foo()
    k.a
}