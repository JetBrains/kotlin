// WITH_STDLIB
// TARGET_BACKEND: JVM_IR

// FILE: J.java
public class J {
    public static Z createZ() {
        return new Z();
    }
}

// FILE: Box.kt
@file:OptIn(ExperimentalStdlibApi::class)
@JvmInline
@JvmExposeBoxed
value class Z(val value: Any = {})

fun box(): String {
    val kotlin = Z().value
    val java = J.createZ().value
    if (kotlin != java) return "$kotlin != $java"
    return "OK"
}