// TARGET_BACKEND: JVM
// FIR_IDENTICAL

// FILE: nnStringVsTXArray.kt
fun <T> useTX(x: T, fn: () -> T) = fn()

fun testWithNullCheck(xs: Array<String>) {
    useTX(xs) { J.notNullString() }
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
