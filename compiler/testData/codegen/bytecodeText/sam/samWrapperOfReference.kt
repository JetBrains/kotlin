// SAM_CONVERSIONS: CLASS
// FILE: JFoo.java

public class JFoo {
    public static void foo(Runnable f) {
        f.run();
    }
}

// FILE: Test.kt
class A {
    fun f() {}
}

fun test() {
    JFoo.foo(A()::f)
}

// 1 NEW A
// 2 NEW
// 0 INVOKEINTERFACE
