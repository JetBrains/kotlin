// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: Java1.java
public class Java1 {
    public static <T> void foo(T t) { }
    public static <T> T bar() {
        return null;
    }
    public static <T> void foo(T t, T t2) {}
}

// FILE: Java2.java
public interface Java2 {
    static int bar() {
        return 1;
    }
    static void foo(int t) { }
    static void foo(int t, int t2) {}
}

// FILE: Java3.java
public interface Java3 {
    default Number bar() {
        return 1;
    }
    default void foo(Number t) { }
    default void foo(Number t, Number t2) { }
}

// FILE: Java4.java
public class Java4 extends Java1 { }

// FILE: 1.kt
class A : Java1(), Java2, KotlinInterface { //Kotlin ← Java1, Java2, Kotlin2
    fun test1() = foo(1)
    fun test2(): Int = bar()
    fun test3() = foo("")
    fun test4() = foo(1.5)
    fun test5() = foo(1.5, 8)
}

class B : Java1(), Java2, Java3 {           //Kotlin ← Java1, Java2, Java3
    fun test1() = foo(1)
    fun test2()= bar<Int>()
    fun test3()= bar()
    fun test4() = foo("")
    fun test5() = foo(1.5)
    fun test6() = foo(1.5, 8)
    fun test7() = foo(1, 8)
}

class C : KotlinClass() , Java2 {           //Kotlin ← Java, Kotlin2 ← Java2
    fun test1() = foo(1)
    fun test2(): Int = bar()
    fun test3() = foo("")
    fun test4() = foo(1.5)
    fun test5() = foo(1.5, 8)
    fun test6() = foo(1, 8)
}

class D : Java4(), Java2 {                  //Kotlin ← Java1, Java2 ← Java3
    fun test1() = foo(1)
    fun test2(): Int = bar()
    fun test3() = foo("")
    fun test4() = foo(1.5)
    fun test5() = foo(1.5, 8)
    fun test6() = foo(1, 8)
}

interface KotlinInterface {
    fun <T> foo(t: T) { }
    fun <T> bar(): T {
        return null!!
    }
    fun foo(t: Number){ }
    fun foo(t: Number, t2: Int = 7) { }
}

open class KotlinClass : Java1()

fun test(a: A, b: B, c: C, d: D) {
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
    b.test6()
    b.test7()
    c.test1()
    c.test2()
    c.test3()
    c.test4()
    c.test5()
    c.test6()
    d.test1()
    d.test2()
    d.test3()
    d.test4()
    d.test5()
    d.test6()
}