// WITH_STDLIB
// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JVM_IR
// FILE: test.kt
fun box() {
    val map: Map<String, String> = mapOf("1" to "23")

    for
            (
    e
    in
    map
    )
    {
        e.key + e.value
    }
}

// EXPECTATIONS

// EXPECTATIONS JVM
// test.kt:6 box:
// test.kt:12 box: map:java.util.Map=java.util.Collections$SingletonMap
// test.kt:8 box: map:java.util.Map=java.util.Collections$SingletonMap
// test.kt:15 box: map:java.util.Map=java.util.Collections$SingletonMap, e:java.util.Map$Entry=java.util.AbstractMap$SimpleImmutableEntry
// test.kt:8 box: map:java.util.Map=java.util.Collections$SingletonMap
// test.kt:17 box: map:java.util.Map=java.util.Collections$SingletonMap

// EXPECTATIONS JVM_IR
// test.kt:6 box:
// test.kt:12 box: map:java.util.Map=java.util.Collections$SingletonMap
// test.kt:10 box: map:java.util.Map=java.util.Collections$SingletonMap
// test.kt:15 box: map:java.util.Map=java.util.Collections$SingletonMap, e:java.util.Map$Entry=java.util.AbstractMap$SimpleImmutableEntry
// test.kt:8 box: map:java.util.Map=java.util.Collections$SingletonMap
// test.kt:17 box: map:java.util.Map=java.util.Collections$SingletonMap
