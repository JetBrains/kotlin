// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: Java1.java
public abstract class Java1 implements KotlinInterface {
    public static int a = 2;
    public static void foo(Object t) { }
}

// FILE: Java2.java
public class Java2 implements KotlinInterface {
    public static int a = 2;
    public static void foo(Object t) { }

    @Override
    public int getA() {
        return a;
    }

    @Override
    public void foo(int t) {}

    @Override
    public int bar() {
        return 0;
    }
}

// FILE: test.kt
abstract class A : Java1()

class B : Java1() {
    override val a: Int
        get() = 5

    override fun foo(t: Int) {}

    override fun bar(): Int {
        return 1
    }
}

class C : Java2()

class D : Java2() {
    override val a: Int
        get() = 5
    override fun foo(t: Int) { }
    override fun bar(): Int {
        return 1
    }
}

interface KotlinInterface{
    val a : Int
    fun foo(t: Int)
    fun bar(): Int
}

fun test(a: A, b: B, c: C, d: D){
    a.a
    a.foo(1)
    a.bar()
    b.a
    b.foo(1)
    b.bar()
    c.a
    c.foo(1)
    c.bar()
    d.a
    d.foo(1)
    d.bar()
}