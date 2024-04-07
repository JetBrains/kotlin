// SKIP_KT_DUMP
// FIR_IDENTICAL
// TARGET_BACKEND: JVM

// FILE: Java1.java
public class Java1 {
    public final void foo(){};
    public native void foo2();
    public synchronized void foo3(){};
}

// FILE: Java2.java
public interface Java2 {
    public abstract void foo();
}

// FILE: Java3.java
public class Java3 extends KotlinClass { }

// FILE: Java4.java
public interface Java4 extends KotlinInterface { }

// FILE: Java5.java
public class Java5 extends KotlinClass {
    public native void foo2();
    public synchronized void foo3(){};
}

// FILE: 1.kt

class A : Java1()   //Kotlin ← Java with final, native, synchronized

abstract class B : Java2 //Kotlin ← Java with abstract

class C : Java1() {
    override fun foo2() { }
    override fun foo3() { }
}

class D: Java2 {
    override fun foo() { }
}

class E : Java3()   //Kotlin ← Java ← Kotlin ← Java with final, native, synchronized

abstract class F : Java4    //Kotlin ← Java ← Kotlin ← Java with abstract

class G : Java3() {
    override fun foo2() {}
    override fun foo3() {}
}

class H: Java4 {
    override fun foo() { }
}

class I : Java5()    //Kotlin ← Java ← Kotlin ← Java with explicit override in java

class J : Java5() { //Kotlin ← Java ← Kotlin ← Java with explicit override
    override fun foo2() { }
    override fun foo3() { }
}

open class KotlinClass: Java1()

interface KotlinInterface : Java2

fun test(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J) {
    a.foo()
    a.foo2()
    a.foo3()

    b.foo()

    c.foo()
    c.foo2()
    c.foo3()

    d.foo()

    e.foo()
    e.foo2()
    e.foo3()

    f.foo()

    g.foo()
    g.foo2()
    g.foo3()

    h.foo()

    i.foo()
    i.foo2()
    i.foo3()

    j.foo()
    j.foo2()
    j.foo3()
}