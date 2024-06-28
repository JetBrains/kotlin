// TARGET_BACKEND: JVM
// FIR_IDENTICAL

// FILE: stringVsTXString.kt
fun <T> useTX(x: T, fn: () -> T) = fn()

fun testNoNullCheck() {
    useTX("") { J.string() }
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
