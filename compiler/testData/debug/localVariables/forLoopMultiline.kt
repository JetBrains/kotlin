// WITH_STDLIB
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

// EXPECTATIONS JVM
// test.kt:5 box:
// test.kt:11 box: map:java.util.Map=java.util.Collections$SingletonMap
// test.kt:7 box: map:java.util.Map=java.util.Collections$SingletonMap
// test.kt:14 box: map:java.util.Map=java.util.Collections$SingletonMap, e:java.util.Map$Entry=java.util.AbstractMap$SimpleImmutableEntry
// test.kt:7 box: map:java.util.Map=java.util.Collections$SingletonMap
// test.kt:16 box: map:java.util.Map=java.util.Collections$SingletonMap

// EXPECTATIONS JVM_IR
// test.kt:5 box:
// test.kt:11 box: map:java.util.Map=java.util.Collections$SingletonMap
// test.kt:7 box: map:java.util.Map=java.util.Collections$SingletonMap
// test.kt:14 box: map:java.util.Map=java.util.Collections$SingletonMap, e:java.util.Map$Entry=java.util.AbstractMap$SimpleImmutableEntry
// test.kt:7 box: map:java.util.Map=java.util.Collections$SingletonMap
// test.kt:16 box: map:java.util.Map=java.util.Collections$SingletonMap

// EXPECTATIONS JS_IR
// test.kt:5 box:
// test.kt:5 box:
// test.kt:11 box: map=kotlin.collections.HashMap
// test.kt:11 box: map=kotlin.collections.HashMap
// test.kt:14 box: map=kotlin.collections.HashMap, e=EntryRef
// test.kt:14 box: map=kotlin.collections.HashMap, e=EntryRef
// test.kt:11 box: map=kotlin.collections.HashMap, e=EntryRef
// test.kt:16 box: map=kotlin.collections.HashMap, e=EntryRef
