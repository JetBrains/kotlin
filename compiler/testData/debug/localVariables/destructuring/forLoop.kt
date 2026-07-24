// WITH_STDLIB

// FILE: test.kt
fun box() {
    val map: Map<String, String> = mapOf("1" to "23")
    for ((a, b) in map) {
        a + b
    }
}

// EXPECTATIONS JVM_IR
// test.kt:5 box:
// test.kt:6 box: map:java.util.Map=java.util.Collections$SingletonMap
// test.kt:7 box: map:java.util.Map=java.util.Collections$SingletonMap, a:java.lang.String="1":java.lang.String, b:java.lang.String="23":java.lang.String
// test.kt:6 box: map:java.util.Map=java.util.Collections$SingletonMap
// test.kt:9 box: map:java.util.Map=java.util.Collections$SingletonMap

// EXPECTATIONS JS_IR
// test.kt:5 box:
// test.kt:5 box:
// test.kt:6 box: map=kotlin.collections.HashMap
// test.kt:6 box: map=kotlin.collections.HashMap
// test.kt:7 box: map=kotlin.collections.HashMap, a="1":kotlin.String, b="23":kotlin.String
// test.kt:6 box: map=kotlin.collections.HashMap, a="1":kotlin.String, b="23":kotlin.String
// test.kt:9 box: map=kotlin.collections.HashMap, a="1":kotlin.String, b="23":kotlin.String

// EXPECTATIONS WASM
// test.kt:5 $box: $map:(ref null $kotlin.Any)=null, $IS_INTERFACE_PARAMETER:(ref null $kotlin.Any)=null, $a:(ref null $kotlin.String)=null, $b:(ref null $kotlin.String)=null (41, 41, 41, 48, 48, 48, 41, 35)
// test.kt:6 $box: $map:(ref $kotlin.collections.HashMap)=(ref $kotlin.collections.HashMap), $IS_INTERFACE_PARAMETER:(ref $kotlin.collections.EntryRef)=(ref $kotlin.collections.EntryRef), $a:(ref $kotlin.String)=(ref $kotlin.String), $b:(ref null $kotlin.String)=null (19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13)
// test.kt:7 $box: $map:(ref $kotlin.collections.HashMap)=(ref $kotlin.collections.HashMap), $IS_INTERFACE_PARAMETER:(ref $kotlin.collections.EntryRef)=(ref $kotlin.collections.EntryRef), $a:(ref $kotlin.String)=(ref $kotlin.String), $b:(ref $kotlin.String)=(ref $kotlin.String) (8, 12, 8, 8, 8)
// test.kt:6 $box: $map:(ref $kotlin.collections.HashMap)=(ref $kotlin.collections.HashMap), $IS_INTERFACE_PARAMETER:(ref $kotlin.collections.EntryRef)=(ref $kotlin.collections.EntryRef), $a:(ref $kotlin.String)=(ref $kotlin.String), $b:(ref $kotlin.String)=(ref $kotlin.String) (19, 19, 19, 19, 19, 19, 19, 19, 19, 19)
// test.kt:7 $box: $map:(ref $kotlin.collections.HashMap)=(ref $kotlin.collections.HashMap), $IS_INTERFACE_PARAMETER:(ref $kotlin.collections.EntryRef)=(ref $kotlin.collections.EntryRef), $a:(ref $kotlin.String)=(ref $kotlin.String), $b:(ref $kotlin.String)=(ref $kotlin.String) (8)
// test.kt:9 $box: $map:(ref $kotlin.collections.HashMap)=(ref $kotlin.collections.HashMap), $IS_INTERFACE_PARAMETER:(ref $kotlin.collections.EntryRef)=(ref $kotlin.collections.EntryRef), $a:(ref $kotlin.String)=(ref $kotlin.String), $b:(ref $kotlin.String)=(ref $kotlin.String) (1, 1)
