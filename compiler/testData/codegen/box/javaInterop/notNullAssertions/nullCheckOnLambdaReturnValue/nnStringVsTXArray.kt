// TARGET_BACKEND: JVM
// FILE: nnStringVsTXArray.kt
fun <T> useTX(x: T, fn: () -> T) = fn()

fun box(): String {
    val xs = Array(1) { "" }
    try {
        useTX(xs) { J.notNullString() }
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
