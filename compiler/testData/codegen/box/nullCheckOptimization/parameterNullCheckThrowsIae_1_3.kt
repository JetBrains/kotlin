// KOTLIN_CONFIGURATION_FLAGS: +JVM.NO_UNIFIED_NULL_CHECKS
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
        return "Fail: IAE should have been thrown"
    } catch (e: Throwable) {
        if (e::class != IllegalArgumentException::class) return "Fail: exception class should be IAE: ${e::class}"
        return "OK"
    }
}
