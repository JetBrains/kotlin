// !LANGUAGE: +StrictJavaNullabilityAssertions
// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM

// FILE: box.kt
fun box(): String {
    try {
        J().test()
        return "Fail: should throw"
    }
    catch (e: Throwable) {
        return "OK"
    }
}

// FILE: test.kt
fun withAssertion(j: J) {
    val x = j.nullString()
}

// FILE: J.java
import org.jetbrains.annotations.NotNull;

public class J {
    public @NotNull String nullString() {
        return null;
    }

    public void test() {
        TestKt.withAssertion(this);
    }
}