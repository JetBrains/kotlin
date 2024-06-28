// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: Java1.java
public interface Java1 {
    static Object a = 1;
    static void foo(Object t) { }
    static Object bar() {
        return null;
    }
}

// FILE: Java2.java
public class Java2 implements KotlinInterface { }

// FILE: Java3.java
public class Java3 implements KotlinInterface {
    public static int a = 3;
    public static void foo(int t) { }
    public static int bar() {
        return 0;
    }
}

// FILE: Java4.java
public abstract class Java4 implements KotlinInterface2 { }

// FILE: Java5.java
public class Java5 implements KotlinInterface2 {
    public static int a = 5;
    public static void foo(Object t) { }

    @Override
    public int getA() {
        return a;
    }

    @Override
    public void foo(int t) { }

    @Override
    public int bar() {
        return 1;
    }
}

// FILE: 1.kt
class A : Java2()   //Kotlin ← Java ← Kotlin ← Java(static)

class B : Java2() {
    val a = 10
    fun foo(t: Int) { }
    fun bar(): Int {
        return 10
    }
}

class C : Java3()   //Kotlin ← Java(static) ← Kotlin ← Java(static)

class D : Java3() {
    val a = 10
    fun bar(): Any{
        return 10
    }
    fun foo(t: Any) { }
}

abstract class E : Java4()  //Kotlin ← Java ← Kotlin(funs with same signature) ← Java(static)

class F(override val a: Int) : Java4() {
    override fun bar(): Int {
        return 10
    }
    override fun foo(t: Int) { }
}

class G : Java5()   //Kotlin ← Java(override + static) ← Kotlin(funs with same signature) ← Java(static)

class H : Java5() {
    override val a: Int
        get() = 10

    override fun bar(): Int {
        return 10
    }
    override fun foo(t: Int) { }
}

interface KotlinInterface : Java1

interface KotlinInterface2: Java1 {
    val a: Int
    fun foo(t: Int) { }
    fun bar(): Int {
        return 10
    }
}

fun test(a:A, b: B, c: C, d: D, e: E, f: F, g: G, h: H){
    b.a
    b.foo(1)
    b.bar()
    d.a
    d.foo("")
    d.bar()
    e.a
    e.bar()
    e.foo(1)
    f.a
    f.foo(1)
    f.bar()
    g.a
    g.foo(1)
    g.bar()
    h.a
    h.foo(1)
    h.bar()
}