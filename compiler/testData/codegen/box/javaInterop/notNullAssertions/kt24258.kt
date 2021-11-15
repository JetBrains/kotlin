// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// WITH_STDLIB
// FILE: kt24258.kt

val lazyNullString: String by lazy { J.nullString() }


fun testLazyNullString() {
    try {
        val s: String = lazyNullString
        throw Exception("'val s: String = lazyNullString' should throw NullPointerException")
    } catch (e: NullPointerException) {
    }
}

fun box(): String {
    testLazyNullString()

    return "OK"
}

// FILE: J.java
public class J {
    public static String nullString() {
        return null;
    }
}