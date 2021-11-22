// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 java/lang/invoke/LambdaMetafactory

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
