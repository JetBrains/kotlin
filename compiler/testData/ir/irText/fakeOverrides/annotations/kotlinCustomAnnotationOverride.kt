// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// ISSUE: KT-65388

// FILE: Java1.java
public class Java1 extends A {
    public Java1(int i) {
        super(i);
    }
}

// FILE: Java2.java
public class Java2 extends A  {
    public Java2(int i) {
        super(i);
    }
    @Override
    public void foo(int a) { }

    @Override
    public int getA() {
        return 5;
    }
}

// FILE: Java3.java
interface Java3 {
    public void foo(int a);
    public void setA(int a);
}

// FILE: 1.kt
annotation class MyAnnotation

open class A {
    @MyAnnotation
    open fun foo(@MyAnnotation a: Int) { }

    @MyAnnotation
    open val a : Int = 1

    @MyAnnotation constructor(i: Int)

    open var b
        @MyAnnotation get() = 10
        @MyAnnotation set(value) {}
}

class B: Java1(1)   //Kotlin ← Java ← Kotlin

class C : Java1(1) {
    override val a: Int
        get() = 10

    override fun foo(a: Int) { }
    override var b: Int
        get() = 11
        set(value) {}
}

class D: Java2(2)   //Kotlin ← Java (override) ← Kotlin

class E : Java2(2) {
    override val a: Int
        get() = 10
    override var b: Int = 0
        get() = 11
    override fun foo(a: Int) { }
}

abstract class F : Java1(1), Java3  // Kotlin ← Java1, Java2 ← Kotlin2

class G : Java1(1), Java3 {
    override fun setA(a: Int) { }
    override fun foo(a: Int) { }
}

class H : Java1(1), KotlinInterface // Kotlin ← Java, Kotlin2 ← Kotlin3

class I : Java1(1), KotlinInterface {
    override var b: Int
        get() = 2
        set(value) {}
    override val a: Int
        get() = 2

    override fun foo(a: Int) { }
}

interface KotlinInterface {
    val a : Int
    val b : Int
    fun foo(a: Int)
}

fun test(b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I) {
    b.a
    b.b
    b.foo(1)
    c.a
    c.b
    c.foo(1)
    d.a
    d.b
    d.foo(1)
    e.a
    e.b
    e.foo(1)
    f.a
    f.b
    f.foo(1)
    g.a
    g.b
    g.foo(1)
    h.a
    h.b
    h.foo(1)
    i.a
    i.b
    i.foo(1)
}
