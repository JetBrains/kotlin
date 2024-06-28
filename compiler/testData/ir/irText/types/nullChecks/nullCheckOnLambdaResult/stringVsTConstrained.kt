// FIR_IDENTICAL
// TARGET_BACKEND: JVM

// FILE: stringVsTConstrained.kt
fun <T> useTConstrained(xs: Array<T>, fn: () -> T) = fn()

fun testWithNullCheck(xs: Array<String>) {
    useTConstrained(xs) { J.string() }
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
