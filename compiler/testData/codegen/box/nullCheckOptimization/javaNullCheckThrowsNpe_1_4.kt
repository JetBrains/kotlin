// !API_VERSION: LATEST
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM_IR
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
        return "Fail: NPE should have been thrown"
    } catch (e: Throwable) {
        if (e::class != NullPointerException::class) return "Fail: exception class should be NPE: ${e::class}"
        return "OK"
    }
}
