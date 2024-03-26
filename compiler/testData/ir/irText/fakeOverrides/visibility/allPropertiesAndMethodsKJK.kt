// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// SEPARATE_SIGNATURE_DUMP_FOR_K2
// ^ ISSUE: KT-65679

// FILE: Java1.java
public class Java1 extends PublicVisibility { }

// FILE: Java2.java
public class Java2 extends PrivateVisibility { }

// FILE: Java3.java
public class Java3 extends ProtectedVisibility { }

// FILE: Java4.java
public class Java4 extends DefaultVisibility { }

// FILE: Java5.java
public class Java5 extends InternalVisibility { }

// FILE: Java6.java
public class Java6 extends PublicVisibility {
    public int a = 5;
    public void foo(){}
}

// FILE: Java7.java
public class Java7 extends InternalVisibility {
    public int a = 7;
    public void foo(){}
}

// FILE: Java8.java
public class Java8 extends ProtectedVisibility {
    public int a = 8;
    public void foo() {}
}

// FILE: Java9.java
public class Java9 extends PrivateVisibility {
    public int a = 9;
    public void foo() {}
}

// FILE: Java10.java
public class Java10 extends DefaultVisibility {
    public int a = 10;
    public void foo() {}
}

// FILE: Java11.java
public class Java11 extends ProtectedVisibility {
    protected int a = 11;
    protected void foo() {}
}

// FILE: Java12.java
public class Java12 extends InternalVisibility {
    protected int a = 12;
    protected void foo() {}
}

// FILE: Java13.java
public class Java13 extends PrivateVisibility {
    protected int a = 13;
    protected void foo() {}
}

// FILE: Java14.java
public class Java14 extends PrivateVisibility {
    private int a = 14;
    private void foo() {}
}

// FILE: Java15.java
public class Java15 extends PrivateVisibility {
    int a = 15;
    void foo() {}
}

// FILE: Java16.java
public class Java16 extends InternalVisibility {
    private int a = 16;
    private void foo() {}
}

// FILE: Java17.java
public class Java17 extends InternalVisibility {
    int a = 17;
    void foo(){}
}

// FILE: test.kt
class A : Java1()   //public

class B : Java1() {
    override fun foo() {}
}

class C : Java2()   //private

class D : Java2() {
    fun foo() { }
    val a: Int = 55
}

class E : Java3()   //protected

class F : Java3() {
    public override fun foo() {}
    public override val a: Int
        get() = 55
}

class G : Java4()   //default

class H : Java4() {
    override fun foo() {}
    override val a: Int
        get() = 55
}

class I : Java5()   // internal

class J : Java6()    //public + public

class K : Java6() {
    override fun foo() {}
    override val a: Int
        get() = 55
}

class L : Java7()   //public + internal

class M : Java8()   //public + protected

class N : Java8() {
    public override fun foo() {}
    override val a: Int
        get() = 55
}

class O : Java9()   //public + private

class P : Java9() {
    public override fun foo() {}
}

class Q : Java10()  //public + default

class R : Java10() {
    public override fun foo() {}
    override val a: Int
        get() = 55
}

class S : Java11()   //protected + protected

class T : Java11() {
    public override fun foo() {}
    override val a: Int
        get() = 55
}

class U : Java12()  //protected + internal

class V : Java12() {
    public override fun foo() {}
}

class W : Java13()  //protected + private

class X : Java13() {
    public override fun foo() {}
}

class Y : Java14()  //private + private

class Z : Java15()  //private + default

class AA : Java15() {
    protected override fun foo() {}
}

class BB : Java16()  //private + internal

class CC : Java17() //default + internal

class DD : Java17() {
    internal override fun foo() {}
}

open class PublicVisibility {
    public open val a: Int = 1
    public open fun foo() {}
}

open class PrivateVisibility {
    private val a: Int = 2
    private fun foo() { }
}

open class ProtectedVisibility {
    protected open val a: Int = 3
    protected open fun foo() { }
}

open class InternalVisibility {
    internal open val a: Int = 4
    internal open fun foo() { }
}

open class DefaultVisibility {
    open val a: Int = 4
    open fun foo() { }
}

fun test(
    a: A, b: B, d: D, f: F, g: G, h: H, j: J, k: K, l: L, m: M, n: N, o: O, p: P,
    q: Q, r: R, s: S, t: T, u: U, v: V, w: W, x: X, aa: AA, cc: CC, dd: DD
) {
    a.a
    a.foo()
    b.a
    b.foo()
    d.a
    d.foo()
    f.a
    f.foo()
    g.a
    g.foo()
    h.a
    h.foo()
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
    o.a
    o.foo()
    p.a
    p.foo()
    q.a
    q.foo()
    r.a
    r.foo()
    s.a
    s.foo()
    t.a
    t.foo()
    u.a
    u.foo()
    v.a
    v.foo()
    w.a
    w.foo()
    x.a
    x.foo()
    aa.a
    cc.a
    cc.foo()
    dd.a
    dd.foo()
}
