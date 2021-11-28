// TARGET_BACKEND: JVM
// WITH_STDLIB

// FILE: flexibleNullability.kt
fun testEmpty() {
    when (J.nullString()) {
    }
}

fun testSingle() {
    var q = "other"
    when (J.nullString()) {
        "a" -> q = "A"
    }
    if (q != "other") {
        throw Exception("Expected 'other', got '$q'")
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
    val q = when (J.nullString()) {
        "a" -> "A"
        else -> "other"
    }
    if (q != "other") {
        throw Exception("Expected 'other', got '$q'")
    }
}

fun testBigger() {
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
    if (q != "other") {
        throw Exception("Expected 'other', got '$q'")
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
public class J {
    public static String nullString() { return null; }
}
