// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 0 java/lang/invoke/LambdaMetafactory
// 1 final class EnhancedNullabilityKt\$box\$t1\$1
//  TODO check why EnhancedNullabilityKt\$box\$t1\$1 is not synthetic

// FILE: enhancedNullability.kt
fun interface IGetInt {
    fun get(x: Int): Int
}

// fun interface IGetIntMix0 : IGetInt, Sam
// ^ Error: [FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS] Fun interfaces must have exactly one abstract method

fun interface IGetIntMix1 : IGetInt, Sam {
    override fun get(x: Int): Int
}

fun box(): String {
    val t1 = IGetIntMix1 { it: Int -> it * 2 }.get(21)
    if (t1 != 42)
        return "Failed: t1=$t1"

    return "OK"
}

// FILE: Sam.java
import org.jetbrains.annotations.*;

public interface Sam {
    @NotNull Integer get(@NotNull Integer arg);
}
