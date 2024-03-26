// SKIP_KT_DUMP
// FIR_IDENTICAL
// TARGET_BACKEND: JVM

// FILE: Java1.java
public class Java1 {
    public static int a = 2;
    public static void foo(Object t) { }
    public static Object bar() {
        return null;
    }
}
// FILE: test.kt

abstract class A : Java1(),  KotlinInterface

class B(override val a: Int) : Java1(), KotlinInterface {
    override fun foo(t: Int) { }
    override fun bar(): Int {
        return 1
    }
}

interface KotlinInterface {
    val a : Int
    fun foo(t: Int)
    fun bar(): Int
}

fun test(a: A, b: B){
    a.a
    a.foo(1)
    a.bar()
    b.a
    b.foo(1)
    b.bar()
}