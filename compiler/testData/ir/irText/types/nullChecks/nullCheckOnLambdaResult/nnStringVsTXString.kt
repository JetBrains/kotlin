// TARGET_BACKEND: JVM
// FIR_IDENTICAL

// NO_SIGNATURE_DUMP
// ^KT-57428

// FILE: nnStringVsTXString.kt
fun <T> useTX(x: T, fn: () -> T) = fn()

fun testWithNullCheck {
    useTX("") { J.notNullString() }
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
