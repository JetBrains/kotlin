// !LANGUAGE: +StrictJavaNullabilityAssertions
// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM

// FILE: box.kt
fun box(): String {
    try {
        outer()
        return "Fail: should throw"
    }
    catch (e: Throwable) {
        return "OK"
    }
}

// FILE: test.kt
fun outer() {
    fun withAssertion() = J().nullString()
    withAssertion() // NB not used itself
}

// FILE: J.java
import org.jetbrains.annotations.NotNull;

public class J {
    public @NotNull String nullString() {
        return null;
    }
}