// TARGET_BACKEND: JVM
// FIR_IDENTICAL

// FILE: nnStringVsT.kt
fun <T> useT(fn: () -> T) = fn()

fun testNoNullCheck() {
    useT { J.notNullString() }
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
