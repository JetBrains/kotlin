// FIR_IDENTICAL
// TARGET_BACKEND: JVM
// FILE: J.java
import org.jetbrains.annotations.*;

public class J {
    @NotNull
    public static String foo() {
        return null;
    }
}

// FILE: test.kt
fun bar(): String {
    return try {
        ""
    } catch (e: Exception) {
        J.foo()
    }
}