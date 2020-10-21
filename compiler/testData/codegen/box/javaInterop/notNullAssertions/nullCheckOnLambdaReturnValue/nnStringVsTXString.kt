// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// FILE: nnStringVsTXString.kt
fun <T> useTX(x: T, fn: () -> T) = fn()

fun box(): String {
    try {
        useTX("") { J.notNullString() }
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
