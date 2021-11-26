// TARGET_BACKEND: JVM
// WITH_STDLIB

// FILE: enhancedNullability.kt
fun testEmpty() {
    when (J.nullString()) {
    }
}

fun testSingle() {
    try {
        var q = "other"
        when (J.nullString()) {
            "a" -> q = "A"
        }
        throw Exception("Should throw NPE, got '$q'")
    } catch (e: NullPointerException) {
    }
}

fun testElseOnly() {
    var q = "other"
    when (J.nullString()) {
        else -> q = "A"
    }
    if (q != "A") {
        throw Exception("Expected: 'A', got '$q'")
    }
}

fun testSmall() {
    try {
        val q = when (J.nullString()) {
            "a" -> "A"
            else -> "other"
        }
        throw Exception("Should throw NPE, got '$q'")
    } catch (e: NullPointerException) {
    }
}

fun testBigger() {
    try {
        val q = when (J.nullString()) {
            "a" -> "A"
            "b" -> "B"
            "c" -> "C"
            "d" -> "D"
            "e" -> "E"
            "f" -> "F"
            "g" -> "G"
            "h" -> "H"
            else -> "other"
        }
        throw Exception("Should throw NPE, got '$q'")
    } catch (e: NullPointerException) {
    }
}


fun box(): String {
    testEmpty()
    testSingle()
    testElseOnly()
    testSmall()
    testBigger()
    return "OK"
}

// FILE: J.java
import org.jetbrains.annotations.NotNull;

public class J {
    public static @NotNull String nullString() { return null; }
}
