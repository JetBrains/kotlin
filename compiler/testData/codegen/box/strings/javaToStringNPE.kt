// TARGET_BACKEND: JVM
// WITH_STDLIB

// FILE: J.java
import org.jetbrains.annotations.NotNull;

public class J {
    @NotNull
    public static String notNullStringIsNull() {
        return null;
    }
}

// FILE: test.kt
fun box(): String {
    try {
        J.notNullStringIsNull().toString()
    } catch (e: java.lang.NullPointerException) {
        return "OK"
    }
    return "Fail"
}
