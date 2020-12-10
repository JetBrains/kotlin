// LANGUAGE: +InlineClasses
// TARGET_BACKEND: JVM
// WITH_RUNTIME

// FILE: WithInlineClass.java

import kotlin.UInt;
import org.jetbrains.annotations.NotNull;

public class WithInlineClass {
    private static UInt UINT = null;

    public static void acceptsUInt(@NotNull UInt u) {
        UINT = u;
    }

    @NotNull
    public static UInt provideUInt() {
        return UINT;
    }
}

// FILE: box.kt

fun box(): String {
    WithInlineClass.acceptsUInt(1u)
    val res = WithInlineClass.provideUInt()
    return if (res == 1u) "OK" else "FAIL $res"
}