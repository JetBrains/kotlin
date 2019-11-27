// !API_VERSION: LATEST
// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// FILE: A.java

public class A {
    public static void test() {
        new B().foo(null);
    }
}

// FILE: test.kt

class B {
    fun foo(s: String) {}
}

fun box(): String {
    try {
        A.test()
        return "Fail: NPE should have been thrown"
    } catch (e: Throwable) {
        if (e::class != NullPointerException::class) return "Fail: exception class should be NPE: ${e::class}"
        return "OK"
    }
}
