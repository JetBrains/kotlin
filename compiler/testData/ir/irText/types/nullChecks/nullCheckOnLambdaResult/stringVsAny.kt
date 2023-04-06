// TARGET_BACKEND: JVM
// FIR_IDENTICAL

// FILE: stringVsAny.kt
fun useAny(fn: () -> Any) = fn()

fun testNullCheck() {
    useAny { J.string() }
}

// FILE: J.java
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class J {
    public static String string() {
        return null;
    }

    public static @NotNull String notNullString() {
        return null;
    }
}
