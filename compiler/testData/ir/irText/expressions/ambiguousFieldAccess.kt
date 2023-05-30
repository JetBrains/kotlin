// TARGET_BACKEND: JVM_IR
// SKIP_KLIB_TEST
// Related to KT-49507

// FILE: A.java
public class A {
    protected String x = "1";
    protected String y = "2";
    public static class B extends A {
        protected String y = "3";
    }
}

// FILE: test.kt

package test

fun <T> eval(f: () -> T) = f()

class C : A.B() {
    // Both x & y here should in fact be taken from B class: this.(super<B>.x), this.(super<B>.y)
    fun f() = eval { x }
    fun g() = eval { y }
}

