// FILE: enhancedNullability.kt
fun use(s: String) {}

fun testUse() {
    use(J.s())
}

fun testLocalVal() {
    val local = J.s()
}

fun testReturnValue() = J.s()

val testGlobalVal = J.s()

val testGlobalValGetter get() = J.s()

// FILE: J.java
import org.jetbrains.annotations.*

public class J {
    public static @NotNull String s() { return null; }
}
