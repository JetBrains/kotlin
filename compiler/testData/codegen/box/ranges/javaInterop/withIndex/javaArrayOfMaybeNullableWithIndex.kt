// !LANGUAGE: +StrictJavaNullabilityAssertions
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

// FILE: box.kt
import kotlin.test.*

fun box(): String {
    val actualIndices = mutableListOf<Int>()
    val actualValues = mutableListOf<Int>()
    for ((index, i) in J.arrayOfMaybeNullable().withIndex()) {
        actualIndices += index
        actualValues += i
    }
    assertEquals(listOf(0, 1), actualIndices)
    assertEquals(listOf(42, null), actualValues)
    return "OK"
}

// FILE: J.java
public class J {
    public static Integer[] arrayOfMaybeNullable() {
        return new Integer[] { 42, null };
    }
}
