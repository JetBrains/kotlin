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