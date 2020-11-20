// FILE: nullabilityAnnotationsForReturnType.kt

fun testNotNull(): String = ""
fun testNullable(): String? = ""
fun testJavaNotNull() = J.returnsNotNull()
fun testJavaNullable() = J.returnsNullable()
fun testJavaFlexible() = J.returnsFlexible()

// FILE: J.java
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class J {
    public static @NotNull String returnsNotNull() { return ""; }
    public static @Nullable String returnsNullable() { return ""; }
    public static String returnsFlexible() { return ""; }
}