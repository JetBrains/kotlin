// SKIP_KT_DUMP
// FIR_IDENTICAL
// TARGET_BACKEND: JVM
// FULL_JDK

// FILE: Java1.java
public class Java1 {
    public void foo(Java1 this){};
    public class Inner {
        public void bar(Inner this){};
    }
}

// FILE: Java2.java
public class Java2 extends Java1 { }

// FILE: Java3.java
public class Java3 extends KotlinClass { }

// FILE: Java4.java
public class Java4 extends KotlinClass {
    public void foo(Java4 this){ };
    public class Inner {
        public void bar(Inner this){};
    }
}

// FILE: 1.kt

class A : Java1()   //Kotlin ← Java

class B : Java1() { //Kotlin ← Java with explicit override
    override fun foo() { }
}

class C : Java2()   //Kotlin ← Java1 ←Java2

class D : Java2() { //Kotlin ← Java1 ←Java2 with explicit override
    override fun foo() { }
}

class E : Java3()   //Kotlin ← Java ← Kotlin ← Java

class F : Java3() { //Kotlin ← Java ← Kotlin ← Java with explicit override
    override fun foo() { }
}

class G : Java4()   //Kotlin ← Java ← Kotlin ← Java with explicit override in java

class H : Java4() { //Kotlin ← Java ← Kotlin ← Java with explicit override in java and kotlin
    override fun foo() { }
}

open class KotlinClass: Java1()

fun test(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H) {
    a.foo()
    a.Inner().bar()
    b.foo()
    b.Inner().bar()
    c.foo()
    c.Inner().bar()
    d.foo()
    d.Inner().bar()
    e.foo()
    e.Inner().bar()
    f.foo()
    f.Inner().bar()
    g.foo()
    g.Inner().bar()
    h.foo()
    h.Inner().bar()
}