// !API_VERSION: 1.3
// IGNORE_BACKEND_FIR: JVM_IR
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
