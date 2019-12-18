// !LANGUAGE: +StrictJavaNullabilityAssertions
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

// FILE: box.kt
import kotlin.test.*

fun box(): String {
    val actualIndices = mutableListOf<Int>()
    val actualValues = mutableListOf<Int>()
    for ((index, i) in JImpl().arrayOfNotNull().withIndex()) {
        actualIndices += index
        actualValues += i
    }
    assertEquals(listOf(0, 1), actualIndices)
    assertEquals(listOf(42, -42), actualValues)
    return "OK"
}

interface J {
    fun arrayOfNotNull(): Array<Int>
}

// FILE: JImpl.java
public class JImpl implements J {
    // The only way to get @EnhancedNullability on the array element type (Int) is to override a Kotlin function that
    // returns `Array<Int>` (where Int is not nullable). `@NotNull Integer[]` makes the array not nullable, not String.
    @Override
    public Integer[] arrayOfNotNull() {
        return new Integer[] { 42, -42 };
    }
}
