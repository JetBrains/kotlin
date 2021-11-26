// FIR_IDENTICAL
// TARGET_BACKEND: JVM
// FILE: enhancedNullability.kt
// WITH_JDK
fun use(s: String) {}

fun testUse() {
    use(J.notNullString())
}

fun testLocalVal() {
    val local = J.notNullString()
}

fun testReturnValue() = J.notNullString()

val testGlobalVal = J.notNullString()

val testGlobalValGetter get() = J.notNullString()

fun testJUse() {
    J.use(J.nullString())
    J.use(J.notNullString())
    J.use(42)
}

fun testLocalVarUse() {
    val ns = J.nullString()
    J.use(ns)

    val nns = J.notNullString()
    J.use(nns)
}

// FILE: J.java
import org.jetbrains.annotations.*;

public class J {
    public static void use(@NotNull String s) {}
    public static void use(@NotNull Integer x) {}
    public static String nullString() { return null; }
    public static @NotNull String notNullString() { return null; }
}
