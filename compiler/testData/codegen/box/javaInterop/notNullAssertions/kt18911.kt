// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: kt18911.kt
fun testNullString() {
    try {
        val t1 = J.nullString()::capitalize
        throw Exception("'J.nullString()::capitalize' should throw")
    } catch (e: NullPointerException) {}
}

fun testNotNullString() {
    try {
        val t1 = J.notNullString()::capitalize
        throw Exception("'J.notNullString()::capitalize' should throw")
    } catch (e: NullPointerException) {}
}

fun box(): String {
    testNullString()
    testNotNullString()
    return "OK"
}

// FILE: J.java
import org.jetbrains.annotations.NotNull;

public class J {
    public static String nullString() {
        return null;
    }

    public static @NotNull String notNullString() {
        return null;
    }
}
