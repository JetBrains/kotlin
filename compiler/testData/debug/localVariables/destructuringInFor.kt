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
// test.kt:6 box:
// test.kt:8 box: map:java.util.Map=java.util.Collections$SingletonMap
// test.kt:7 box: map:java.util.Map=java.util.Collections$SingletonMap
// test.kt:9 box: map:java.util.Map=java.util.Collections$SingletonMap, a:java.lang.String="1":java.lang.String, b:java.lang.String="23":java.lang.String
// test.kt:7 box: map:java.util.Map=java.util.Collections$SingletonMap
// test.kt:11 box: map:java.util.Map=java.util.Collections$SingletonMap