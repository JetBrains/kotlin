// TARGET_BACKEND: JVM
// FILE: stringVsT.kt
fun <T> useT(fn: () -> T) = fn()

fun checkNull(x: Any?) {
    if (x != null) throw AssertionError("null expected")
}

fun box(): String {
    checkNull(useT { J.string() })
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
