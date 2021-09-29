// TARGET_BACKEND: JVM
// FILE: safeCallSimplificationFlexibleType.kt

fun String.zap() = "failed"

fun box() =
    J.nullString()?.zap()
        ?: "OK"

// FILE: J.java
public class J {
    public static String nullString() { return null; }
}
