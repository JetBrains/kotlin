// !LANGUAGE: +StrictJavaNullabilityAssertions
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// FULL_JDK

import kotlin.test.*

fun box(): String {
    val map = java.util.LinkedHashMap<Int, Int>()
    map.put(3, 42)
    map.put(14, -42)

    // Even though the type parameters on `map` are not nullable, the `values` property is implemented in Java and therefore there is
    // @EnhancedNullability on its type argument (Int).
    val actualValues = mutableListOf<Int>()
    for (v in map.values) {
        actualValues += v
    }
    assertEquals(listOf(42, -42), actualValues)
    return "OK"
}
