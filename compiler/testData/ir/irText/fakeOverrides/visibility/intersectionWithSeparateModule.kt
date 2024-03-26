// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// MODULE: separate
// FILE: JavaDefaultSeparateModule.java
public interface JavaDefaultSeparateModule {
    int a = 2;
    void foo();
}
// FILE: JavaProtectedSeparateModule.java
public class JavaProtectedSeparateModule {
    protected int a = 22;
    protected void foo() { }
}

// MODULE: main
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
abstract class A: JavaDefaultSeparateModule, KotlinDefault {
    public override fun foo() { }
    public override val a: Int
        get() = 5
}

abstract class B : JavaDefaultSeparateModule, KotlinPrivate

class C : JavaDefaultSeparateModule, KotlinPrivate {
    public override fun foo() {}
    val a = 5
}

class D : JavaDefaultSeparateModule, KotlinProtected() {
    public override fun foo() {}
    protected override val a: Int
        get() = 5
}

class E : JavaDefaultSeparateModule, KotlinPublic {
    public override fun foo() {}
    override val a: Int
        get() = 5
}

class F : JavaDefaultSeparateModule, KotlinInternal() {
    public override fun foo() {}
    public override val a: Int
        get() = 5
}

class G : JavaProtectedSeparateModule(), KotlinDefault {
    public override fun foo() {}
    public override val a: Int
        get() = 5
}

class H : JavaProtectedSeparateModule(), KotlinPrivate

class I : JavaProtectedSeparateModule(), KotlinPrivate {
    public override fun foo() {}
    val a = 5
}

class J : JavaProtectedSeparateModule(), KotlinPublic {
    public override fun foo() {}
    override val a: Int
        get() = 5
}

abstract class K : JavaPublic, JavaDefaultSeparateModule

class L : JavaPublic, JavaDefaultSeparateModule {
    override fun foo() {}
    val a = 1
}

class M : JavaPublic, JavaProtectedSeparateModule() {
    override fun foo() {}
    val a = 1
}

class N : JavaProtectedSeparateModule(), JavaDefault {
    override fun foo() {}
    val a = 1
}

abstract class O : JavaPrivate(), JavaDefaultSeparateModule

class P : JavaPrivate(), JavaDefaultSeparateModule {
    override fun foo() {}
    val a = 1
}

class R : JavaProtected(), JavaDefaultSeparateModule {
    override fun foo() {}
    val a = 1
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

fun test(a: A, b: B, c: C, d: D, e: E, f: F, g: G, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P, r: R) {
    a.foo()
    a.a
    b.foo()
    c.foo()
    d.foo()
    e.foo()
    e.a
    f.foo()
    f.a
    g.foo()
    g.a
    i.foo()
    i.a
    j.foo()
    j.a
    k.foo()
    l.foo()
    l.a
    m.foo()
    m.a
    n.foo()
    n.a
    o.foo()
    p.foo()
    p.a
    r.foo()
    r.a
}