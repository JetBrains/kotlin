// LANGUAGE: +InlineClasses
// TARGET_BACKEND: JVM
// WITH_STDLIB

// FILE: WithInlineClass.java

import kotlin.UInt;
import org.jetbrains.annotations.NotNull;

public class WithInlineClass {
    @NotNull
    public static UInt UINT = null;

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
    var res = WithInlineClass.provideUInt()
    if (res != 1u) return "FAIL 1 $res"
    WithInlineClass.UINT = 2u
    res = WithInlineClass.UINT
    if (res != 2u) return "FAIL 2 $res"
    return "OK"
}