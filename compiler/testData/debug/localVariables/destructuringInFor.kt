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
// TestKt:8: map:java.util.Collections$SingletonMap
// TestKt:7: map:java.util.Collections$SingletonMap
// TestKt:9: map:java.util.Collections$SingletonMap, a:java.lang.String, b:java.lang.String
// TestKt:7: map:java.util.Collections$SingletonMap
// TestKt:11: map:java.util.Collections$SingletonMap
