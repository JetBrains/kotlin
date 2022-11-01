// WITH_STDLIB
// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JVM_IR
// FILE: test.kt
fun box() {
    val map: Map<String, String> = mapOf("1" to "23")

    for
            (
    (
        a
            ,
        b
    )
    in
    map
    )
    {
        a + b
    }
}

// EXPECTATIONS JVM
// test.kt:6 box:
// test.kt:16 box: map:java.util.Map=java.util.Collections$SingletonMap
// test.kt:8 box: map:java.util.Map=java.util.Collections$SingletonMap
// test.kt:11 box: map:java.util.Map=java.util.Collections$SingletonMap
// test.kt:13 box: map:java.util.Map=java.util.Collections$SingletonMap
// test.kt:19 box: map:java.util.Map=java.util.Collections$SingletonMap, a:java.lang.String="1":java.lang.String, b:java.lang.String="23":java.lang.String
// test.kt:8 box: map:java.util.Map=java.util.Collections$SingletonMap
// test.kt:21 box: map:java.util.Map=java.util.Collections$SingletonMap

// EXPECTATIONS JVM_IR
// test.kt:6 box:
// test.kt:16 box: map:java.util.Map=java.util.Collections$SingletonMap
// test.kt:11 box: map:java.util.Map=java.util.Collections$SingletonMap
// test.kt:13 box: map:java.util.Map=java.util.Collections$SingletonMap, a:java.lang.String="1":java.lang.String
// test.kt:19 box: map:java.util.Map=java.util.Collections$SingletonMap, a:java.lang.String="1":java.lang.String, b:java.lang.String="23":java.lang.String
// test.kt:8 box: map:java.util.Map=java.util.Collections$SingletonMap
// test.kt:21 box: map:java.util.Map=java.util.Collections$SingletonMap

// EXPECTATIONS JS_IR
// test.kt:6 box:
// test.kt:6 box:
// test.kt:16 box: map=kotlin.collections.HashMap
// test.kt:16 box: map=kotlin.collections.HashMap
// test.kt:16 box: map=kotlin.collections.HashMap
// test.kt:11 box: map=kotlin.collections.HashMap
// test.kt:13 box: map=kotlin.collections.HashMap, a="1":kotlin.String
// test.kt:19 box: map=kotlin.collections.HashMap, a="1":kotlin.String, b="23":kotlin.String
// test.kt:16 box: map=kotlin.collections.HashMap, a="1":kotlin.String, b="23":kotlin.String
// test.kt:21 box: map=kotlin.collections.HashMap, a="1":kotlin.String, b="23":kotlin.String
