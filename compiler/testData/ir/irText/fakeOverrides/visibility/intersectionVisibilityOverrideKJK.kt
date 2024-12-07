// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: JavaProtected.java
public class JavaProtected {
    protected int a = 3;
    protected void foo() {}
}

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

// FILE: JavaPrivate.java
public class JavaPrivate  {
    private int a = 2;
    private void foo(){}
}

// FILE: test.kt
abstract class A: JavaDefault, KotlinDefault {
    public override fun foo() { }
    public override val a: Int
        get() = 5
    fun test() {
        a
        foo()
    }
}

abstract class B : JavaDefault, KotlinPrivate{
    fun test() {
        foo()
    }
}

class C : JavaDefault, KotlinPrivate {
    public override fun foo() {}
    val a = 5
    fun test() {
        a
        foo()
    }
}

class D : JavaDefault, KotlinProtected() {
    public override fun foo() {}
    protected override val a: Int
        get() = 5
    fun test() {
        a
        foo()
    }
}

class E : JavaDefault, KotlinPublic {
    public override fun foo() {}
    override val a: Int
        get() = 5
    fun test() {
        a
        foo()
    }
}

class F : JavaDefault, KotlinInternal() {
    public override fun foo() {}
    public override val a: Int
        get() = 5
    fun test() {
        a
        foo()
    }
}

class G : JavaPrivate(), KotlinDefault{
    fun test() {
        a
        foo()
    }
}

class H : JavaPrivate(), KotlinDefault {
    override fun foo() {}
    override val a: Int
        get() = 5
    fun test() {
        a
        foo()
    }
}

class I : JavaPrivate(), KotlinPrivate

class J : JavaPrivate(), KotlinPublic {
    fun test() {
        a
        foo()
    }
}

class K : JavaPrivate(), KotlinPublic {
    override fun foo() {}
    override val a: Int
        get() = 5
    fun test() {
        a
        foo()
    }
}

class L : JavaProtected(), KotlinDefault {
    public override fun foo() {}
    public override val a: Int
        get() = 5
    fun test() {
        a
        foo()
    }
}

class M : JavaProtected(), KotlinPrivate {
    fun test() {
        a
        foo()
    }
}

class N : JavaProtected(), KotlinPrivate {
    public override fun foo() {}
    val a = 5
    fun test() {
        a
        foo()
    }
}

class O : JavaProtected(), KotlinPublic {
    public override fun foo() {}
    override val a: Int
        get() = 5
    fun test() {
        a
        foo()
    }
}

class P : JavaPublic, KotlinDefault {
    override fun foo() {}
    override val a: Int
        get() = 5
    fun test() {
        a
        foo()
    }
}

class Q : JavaPublic, KotlinPrivate {
    override fun foo() {}
    val a = 5
    fun test() {
        a
        foo()
    }
}

class R : JavaPublic, KotlinProtected() {
    public override fun foo() {}
    protected override val a: Int
        get() = 5
    fun test() {
        a
        foo()
    }
}

class S : JavaPublic, KotlinPublic {
    override fun foo() {}
    override val a: Int
        get() = 5
    fun test() {
        a
        foo()
    }
}

class T : JavaPublic, KotlinInternal() {
    public override fun foo() {}
    internal override val a: Int
        get() = 5
    fun test() {
        a
        foo()
    }
}

interface KotlinPrivate {
    private val a : Int
        get() = 1
    private fun foo(){}
}

open class KotlinProtected {
    protected open val a : Int = 1
    protected open fun foo(){}
}

interface KotlinDefault {
    val a : Int
        get() = 1
    fun foo(){}
}

interface KotlinPublic {
    public val a : Int
        get() = 1
    public fun foo(){}
}

open class KotlinInternal {
    internal open val a : Int
        get() = 1
    internal open fun foo(){}
}

fun test(a: A, b: B, c: C, d: D, e: E, f: F, h: H, j: J, k: K, l: L, m: M, n: N, o: O, p: P, q: Q, r: R, s: S, t: T){
    a.foo()
    a.a
    a.test()
    b.foo()
    b.test()
    c.foo()
    c.test()
    d.foo()
    d.foo()
    e.foo()
    e.a
    e.test()
    f.foo()
    f.a
    f.test()
    h.foo()
    h.a
    h.test()
    j.foo()
    j.a
    j.test()
    k.foo()
    k.a
    k.test()
    l.foo()
    l.a
    l.test()
    m.foo()
    m.a
    m.test()
    n.foo()
    n.a
    n.test()
    o.foo()
    o.a
    o.test()
    p.foo()
    p.a
    p.test()
    q.foo()
    q.test()
    r.foo()
    r.test()
    s.foo()
    s.test()
    s.a
    t.foo()
    t.test()
    t.a
}
