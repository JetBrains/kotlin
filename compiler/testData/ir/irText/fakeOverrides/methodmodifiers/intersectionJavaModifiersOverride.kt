// SKIP_KT_DUMP
// FIR_IDENTICAL
// TARGET_BACKEND: JVM

// FILE: Java1.java
public interface Java1 {
    public abstract void foo();
}

// FILE: Java2.java
public class Java2 {
    public final void foo(){};
    public native void foo2();
    public synchronized void foo3(){};
}

// FILE: Java3.java
public interface Java3 {
    public void foo();
    public void foo2();
    public void foo3();
}

// FILE: 1.kt

abstract class A : Java1, Java2() //Kotlin ← Java1, Java2

class B : Java2(), Java3

class C : Java2(), Java3 {
    override fun foo2() { }
    override fun foo3() { }
}

abstract class D : Java1, Java3

class E : Java1, Java3 {
    override fun foo() { }
    override fun foo2() { }
    override fun foo3() { }
}

class F : Java2(), KotlinInterface  //Kotlin ← Java, Kotlin2

class G: Java2(), KotlinInterface {
    override fun foo2() { }
    override fun foo3() { }
}

abstract class H : Java1, KotlinInterface

class I : Java1, KotlinInterface {
    override fun foo(){ }
    override fun foo2() { }
    override fun foo3() { }
}

interface KotlinInterface {
    fun foo()
    fun foo2()
    fun foo3()
}

fun test(a: A, b: B, c: C, d: D, e: E, f: F, g: G,h: H, i: I){
    a.foo()
    a.foo2()
    a.foo3()
    b.foo()
    b.foo2()
    b.foo3()
    c.foo()
    c.foo2()
    c.foo3()
    d.foo()
    d.foo2()
    d.foo3()
    e.foo()
    e.foo2()
    e.foo3()
    f.foo()
    f.foo2()
    f.foo3()
    g.foo()
    g.foo2()
    g.foo3()
    h.foo()
    h.foo2()
    h.foo3()
    i.foo()
    i.foo2()
    i.foo3()
}