//FILE: test.kt

// WITH_RUNTIME

fun box() {
    val map: Map<String, String> = mapOf("1" to "23")
    for ((a, b)
        in map) {
        a + b
    }
}
// IGNORE_BACKEND: JVM_IR
// LOCAL VARIABLES
// TestKt:6:
// TestKt:8: LV:map:java.util.Collections$SingletonMap
// TestKt:7: LV:map:java.util.Collections$SingletonMap
// TestKt:9: LV:map:java.util.Collections$SingletonMap, LV:a:java.lang.String, LV:b:java.lang.String
// TestKt:7: LV:map:java.util.Collections$SingletonMap
// TestKt:11: LV:map:java.util.Collections$SingletonMap
