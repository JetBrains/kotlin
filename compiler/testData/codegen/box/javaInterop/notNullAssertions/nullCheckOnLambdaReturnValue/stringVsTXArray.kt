// TARGET_BACKEND: JVM
// FILE: stringVsTXArray.kt
fun <T> useTX(x: T, fn: () -> T) = fn()

fun checkNull(x: Any?) {
    if (x != null) throw AssertionError("null expected")
}

fun box(): String {
    val xs = Array(1) { "" }
    checkNull(useTX(xs) { J.string() })
    return "OK"
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
