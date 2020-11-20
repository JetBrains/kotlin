// KOTLIN_CONFIGURATION_FLAGS: +JVM.NO_UNIFIED_NULL_CHECKS
// TARGET_BACKEND: JVM
// FILE: A.java

import org.jetbrains.annotations.NotNull;

public class A {
    @NotNull
    public static String foo() { return null; }
}

// FILE: test.kt

fun box(): String {
    try {
        val s: String = A.foo()
        return "Fail: ISE should have been thrown"
    } catch (e: Throwable) {
        if (e::class != IllegalStateException::class) return "Fail: exception class should be ISE: ${e::class}"
        return "OK"
    }
}
