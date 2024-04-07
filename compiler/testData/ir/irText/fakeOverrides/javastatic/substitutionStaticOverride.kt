// SKIP_KT_DUMP
// FIR_IDENTICAL
// TARGET_BACKEND: JVM

// FILE: Java1.java
public class Java1 {
    public static <T> void foo(T t) {}
    public static <T> T bar() {
        return null;
    }
    public static  void foo(Number t) {}
    public static  void foo(Number t, Integer t2) {}
}

// FILE: Java2.java
public class Java2 extends Java1 { }

// FILE: Java3.java
public class Java3 extends A { }

// FILE: 1.kt

open class A : Java1() {
    open fun test1() = foo(1)
    open fun test2(): Int = bar()
    open fun test3() = foo("")
    open fun test4() = foo(1, 1)
}

class B: Java2() {                  // Kotlin ← Java1 ← Java2
    fun test1() = foo(1)
    fun test2(): Int = bar()
    fun test3() = foo("")
    fun test4() = foo(1, 1)
}

class C : Java3()                   //Kotlin ← Java ← Kotlin ← Java


fun test(a: A, b: B, c: C) {
    a.test1()
    a.test2()
    a.test3()
    a.test4()
    b.test1()
    b.test2()
    b.test3()
    b.test4()
    c.test1()
    c.test2()
    c.test3()
    c.test4()
}