// SKIP_KT_DUMP
// FIR_IDENTICAL
// TARGET_BACKEND: JVM

// FILE: Java1.java
public class Java1 {
    public static <T> void foo(T t) {}
    public static <T> T bar() {
        return null;
    }
    public static void foo(Number t) {}
    public static void foo(Number t, Integer t2) {}
}

// FILE: Java2.java
public interface Java2 {
    static <T> void foo(T t) {}
    static <T> T bar() {
        return null;
    }
    static void foo(int t) {}
    static void foo(int t, int t2) {}
}

// FILE: 1.kt
class A : Java1(), KotlinInterface {
    fun test1() = foo(1)
    fun test2(): Int = bar()
    fun test3() = foo("")
    fun test4() = foo(1.5)
    fun test5() = foo(1.5, 8)
}

class B : Java1(), Java2 {
    fun test1() = foo(1)
    fun test2(): Int = bar()
    fun test3() = foo("")
    fun test4() = foo(1.5)
    fun test5() = foo(1.5, 8)
}

interface KotlinInterface {
    fun <T> foo(t: T) {}
    fun <T> bar(): T {
        return null!!
    }
    fun foo(t: Number) {}
    fun foo(t: Number, t2: Int = 7) {}
}

fun test(a: A, b: B) {
    a.test1()
    a.test2()
    a.test3()
    a.test4()
    a.test5()
    b.test1()
    b.test2()
    b.test3()
    b.test4()
    b.test5()
}