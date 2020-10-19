// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// FILE: nnStringVsTAny.kt
fun <T : Any> useTAny(fn: () -> T) = fn()

fun checkNull(x: Any?) {
    if (x != null) throw AssertionError("null expected")
}

fun box(): String {
    checkNull(useTAny { J.notNullString() })
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
