// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FILE: Statics.java

public class Statics {
    public static void foo(Runnable r) {}
}

// FILE: test.kt

class A : Statics() {
    fun test() {
        foo {}
    }
}