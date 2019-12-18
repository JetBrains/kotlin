// !LANGUAGE: +StrictJavaNullabilityAssertions
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM, JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

// Note: This fails because explicit types are ignored in destructuring declarations (KT-22392).

// FILE: box.kt
import kotlin.test.*

fun box(): String {
    // Sanity check to make sure there IS an exception even when not in a for-loop
    try {
        val (index, i: Int) = J.arrayOfMaybeNullable().withIndex().first()
        return "Fail: should throw on get()"
    } catch (e: IllegalStateException) {}

    try {
        for ((index, i: Int) in J.arrayOfMaybeNullable().withIndex()) {
            return "Fail: should throw on get() in loop header"
        }
    }
    catch (e: IllegalStateException) {}
    return "OK"
}

// FILE: J.java
public class J {
    public static Integer[] arrayOfMaybeNullable() {
        return new Integer[] { null };
    }
}
