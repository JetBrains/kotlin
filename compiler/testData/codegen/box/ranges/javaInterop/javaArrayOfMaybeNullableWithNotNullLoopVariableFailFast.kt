// !LANGUAGE: +StrictJavaNullabilityAssertions
// TARGET_BACKEND: JVM
// WITH_RUNTIME

// FILE: box.kt
import kotlin.test.*

fun box(): String {
    // Sanity check to make sure there IS an exception even when not in a for-loop
    try {
        val i: Int = J.arrayOfMaybeNullable()[0]
        return "Fail: should throw on get()"
    } catch (e: NullPointerException) {}

    try {
        for (i: Int in J.arrayOfMaybeNullable()) {
            return "Fail: should throw on get() in loop header"
        }
    }
    catch (e: NullPointerException) {}
    return "OK"
}

// FILE: J.java
public class J {
    public static Integer[] arrayOfMaybeNullable() {
        return new Integer[] { null };
    }
}
