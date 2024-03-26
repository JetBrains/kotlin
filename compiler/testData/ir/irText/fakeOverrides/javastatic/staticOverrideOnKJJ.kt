// SKIP_KT_DUMP
// FIR_IDENTICAL
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
public class Java2 implements Java1 { }

// FILE: Java3.java
public class Java3 implements Java1 {
    public static int a = 3;
    public static void foo(int t) { }
    public static int bar() {
        return 0;
    }
}

// FILE: test.kt
class A : Java2()

class B : Java2() {
    val a = 10
    fun foo(t: Int) { }
    fun bar(): Int { return 10 }
}

class C : Java3()

class D : Java3() {
    val a = 10
    fun foo(t: Int) {}
    fun bar(): Int { return 10 }
}

fun test(b: B, d: D){
    b.a
    b.foo(1)
    b.bar()
    d.a
    d.foo(1)
    d.bar()
}