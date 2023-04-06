// TARGET_BACKEND: JVM
// FIR_IDENTICAL
// DUMP_LOCAL_DECLARATION_SIGNATURES

// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57428

// FILE: nnStringVsTConstrained.kt
fun <T> useTConstrained(xs: Array<T>, fn: () -> T) = fn()

fun testWithNullCheck(xs: Array<String>) {
    useTConstrained(xs) { J.notNullString() }
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
