// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: Java1.java
public interface Java1 {
    static Object a = 1;
    static void foo(Object t) {}
    static Object bar() {
        return null;
    }
}

// FILE: Java2.java
public class Java2 {
    static Integer a = 2;
    static void foo(Integer t) {}
    static Integer bar() {
        return 1;
    }
}

// FILE: Java3.java
public interface Java3 {
    default void foo(int t) {}
}


// FILE: Java4.java
public interface Java4 extends KotlinInterface { }

// FILE: Java5.java
public class Java5 implements Java1 {
    public static int a = 5;
    public static void foo(Object t) { }
}

// FILE: 1.kt
abstract class A : Java1, Java2(), KotlinInterface  //Kotlin ← Java1, Java2, Kotlin2

class B(override var a: Int) : Java1, Java2(), KotlinInterface {
    override fun bar(): Int {
        return 5
    }
    override fun foo(t: Int) { }
}

class C(override var a: Int) : Java1, KotlinInterface, KotlinInterface2 {   //Kotlin ← Java, Kotlin1, Kotlin2
    override fun foo(t: Any) { }

    override fun bar(): Int {
        return 1
    }
}

class D : Java1, Java2(), Java3     //Kotlin ← Java1, Java2, Java3

class E : Java1, Java2(), Java3 {
    override fun foo(t: Int) { }
}

abstract class F : Java1, Java4     //Kotlin ← Java1, Java2 ← Kotlin2

class G : Java1, Java4 {
    override var a: Int
        get() = 10
        set(value) { }
    override fun foo(t: Int) { }
    override fun bar(): Int {
        return 10
    }
}

class H(override var a: Int) : Java4, KotlinInterface2 {    //Kotlin ← Java, Kotlin2 ← Kotlin3
    override fun foo(t: Any) { }
    override fun bar(): Int {
        return 1
    }
    override fun foo(t: Int) { }
}

abstract class I : Java5(), KotlinInterface //Kotlin ← Java, Kotlin2 ← Java2

class J : Java5(), KotlinInterface {
    override var a: Int
        get() = 10
        set(value) {}
}

class K : Java5(), Java3    //Kotlin ← Java1, Java2 ← Java3

class L : Java5(), Java3 {
    override fun foo(t: Int) {}
}

interface KotlinInterface {
    var a: Int
    fun foo(t: Int) { }
    fun bar(): Int  {
        return 10
    }
}

interface KotlinInterface2 {
    val a: Any
    fun foo(t: Any)
    fun bar(): Any
}

fun test(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L){
    a.a
    a.foo(1)
    a.bar()
    b.a
    b.foo(1)
    b.bar()
    c.a
    c.foo(1)
    c.foo("")
    c.bar()
    d.foo(1)
    e.foo(1)
    f.a
    f.foo(1)
    f.bar()
    g.a
    g.foo(1)
    g.bar()
    h.a
    h.foo(1)
    h.foo("")
    h.bar()
    i.a
    i.foo(1)
    i.bar()
    j.a
    j.foo(1)
    j.bar()
    k.foo(1)
    l.foo(1)
}