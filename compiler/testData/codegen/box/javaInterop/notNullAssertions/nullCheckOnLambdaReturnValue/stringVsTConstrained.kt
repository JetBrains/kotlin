// TARGET_BACKEND: JVM
// FILE: stringVsTConstrained.kt
fun <T> useTConstrained(xs: Array<T>, fn: () -> T) = fn()

fun box(): String {
    val xs = Array(1) { "" }
    try {
        useTConstrained(xs) { J.string() }
    } catch (e: NullPointerException) {
        return "OK"
    }
    throw AssertionError("NullPointerException expected")
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
