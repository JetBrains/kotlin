// TARGET_BACKEND: JVM
// FILE: nnStringVsT.kt
fun <T> useT(fn: () -> T) = fn()

fun checkNull(x: Any?) {
    if (x != null) throw AssertionError("null expected")
}

fun box(): String {
    checkNull(useT { J.notNullString() })
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
