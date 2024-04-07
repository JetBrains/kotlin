// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K1: ANY
// SEPARATE_SIGNATURE_DUMP_FOR_K2
// ^ ISSUE: KT-65679

// MODULE: separate
// FILE: KotlinInternal.kt
open class KotlinInternal {
    internal open val a : Int
        get() = 1
    internal open fun foo(){}
}

// MODULE: main()(separate)
// FILE: JavaDefault.java
public interface JavaDefault {
    int a = 2;
    void foo();
}

// FILE: JavaPublic.java
public interface JavaPublic {
    public int a = 1;
    public void foo();
}

// FILE: Java1.java
public class Java1 extends KotlinInternal {}

// FILE: Java2.java
public class Java2 extends KotlinInternal {
    public int a = 2;
    public void foo() {}
}
// FILE: Java3.java
public class Java3 extends KotlinInternal {
    protected int a = 3;
    protected void foo() {}
}

// FILE: Java4.java
public class Java4 extends KotlinInternal {
    private int a = 4;
    private void foo() {}
}

// FILE: Java5.java
public class Java5 extends KotlinInternal {
    int a = 5;
    void foo(){}
}

// FILE: test.kt
class A : Java1()

class B : Java1() {
    override fun foo() {}
    override val a: Int
        get() = 5
}

class C : Java2()

class D: Java2() {
    override fun foo() {}
    override val a: Int
        get() = 5
}

class E : Java3()

class F : Java3() {
    public override fun foo() {}
    override val a: Int
        get() = 5
}

class G : Java4()

class H : Java4() {
    override val a: Int
        get() = 5
}

class I : Java5()

class J : Java5() {
    public override fun foo() {}
    override val a: Int
        get() = 5
}

class K : JavaPublic, KotlinInternal() {
    public override fun foo() {}
    public override val a: Int
        get() = 5
}

class L : JavaDefault, KotlinInternal() {
    public override fun foo() {}
    public override val a: Int
        get() = 5
}


fun test(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L){
    a.foo()
    a.a

    b.foo()
    b.a

    c.foo()
    c.a

    d.foo()
    d.a

    e.foo()
    e.a

    f.foo()
    f.a

    g.a

    h.a

    i.foo()
    i.a

    j.foo()
    j.a

    k.foo()
    k.a

    l.foo()
    l.a
}