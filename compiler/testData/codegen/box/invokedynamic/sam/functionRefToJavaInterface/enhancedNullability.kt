// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// FILE: enhancedNullability.kt
fun mul2(x: Int) = x * 2

fun box(): String {
    val t = Sam(::mul2).get(21)
    if (t != 42)
        return "Failed: t=$t"
    return "OK"
}

// FILE: Sam.java
import org.jetbrains.annotations.*;

public interface Sam {
    @NotNull Integer get(@NotNull Integer arg);
}
