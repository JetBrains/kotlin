// WITH_STDLIB

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

// EXPECTATIONS JVM_IR
// test.kt:5 box:
// test.kt:15 box: map:java.util.Map=java.util.Collections$SingletonMap
// EXPECTATIONS FIR JVM_IR
// test.kt:9 box: map:java.util.Map=java.util.Collections$SingletonMap
// EXPECTATIONS JVM_IR
// test.kt:10 box: map:java.util.Map=java.util.Collections$SingletonMap
// test.kt:12 box: map:java.util.Map=java.util.Collections$SingletonMap, a:java.lang.String="1":java.lang.String
// test.kt:18 box: map:java.util.Map=java.util.Collections$SingletonMap, a:java.lang.String="1":java.lang.String, b:java.lang.String="23":java.lang.String
// test.kt:15 box: map:java.util.Map=java.util.Collections$SingletonMap
// test.kt:20 box: map:java.util.Map=java.util.Collections$SingletonMap

// EXPECTATIONS JS_IR
// test.kt:5 box:
// test.kt:5 box:
// test.kt:15 box: map=kotlin.collections.HashMap
// test.kt:15 box: map=kotlin.collections.HashMap
// test.kt:18 box: map=kotlin.collections.HashMap, a="1":kotlin.String, b="23":kotlin.String
// test.kt:15 box: map=kotlin.collections.HashMap, a="1":kotlin.String, b="23":kotlin.String
// test.kt:20 box: map=kotlin.collections.HashMap, a="1":kotlin.String, b="23":kotlin.String

// EXPECTATIONS WASM
// test.kt:5 $box: $map:(ref null $kotlin.Any)=null, $IS_INTERFACE_PARAMETER:(ref null $kotlin.Any)=null, $a:(ref null $kotlin.String)=null, $b:(ref null $kotlin.String)=null (41, 41, 41, 48, 48, 48, 41, 35)
// test.kt:15 $box: $map:(ref $kotlin.collections.HashMap)=(ref $kotlin.collections.HashMap), $IS_INTERFACE_PARAMETER:(ref $kotlin.collections.EntryRef)=(ref $kotlin.collections.EntryRef), $a:(ref null $kotlin.String)=null, $b:(ref null $kotlin.String)=null (4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4)
// test.kt:10 $box: $map:(ref $kotlin.collections.HashMap)=(ref $kotlin.collections.HashMap), $IS_INTERFACE_PARAMETER:(ref $kotlin.collections.EntryRef)=(ref $kotlin.collections.EntryRef), $a:(ref null $kotlin.String)=null, $b:(ref null $kotlin.String)=null (8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8)
// test.kt:12 $box: $map:(ref $kotlin.collections.HashMap)=(ref $kotlin.collections.HashMap), $IS_INTERFACE_PARAMETER:(ref $kotlin.collections.EntryRef)=(ref $kotlin.collections.EntryRef), $a:(ref $kotlin.String)=(ref $kotlin.String), $b:(ref null $kotlin.String)=null (8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8)
// test.kt:18 $box: $map:(ref $kotlin.collections.HashMap)=(ref $kotlin.collections.HashMap), $IS_INTERFACE_PARAMETER:(ref $kotlin.collections.EntryRef)=(ref $kotlin.collections.EntryRef), $a:(ref $kotlin.String)=(ref $kotlin.String), $b:(ref $kotlin.String)=(ref $kotlin.String) (8, 12, 8, 8, 8)
// test.kt:15 $box: $map:(ref $kotlin.collections.HashMap)=(ref $kotlin.collections.HashMap), $IS_INTERFACE_PARAMETER:(ref $kotlin.collections.EntryRef)=(ref $kotlin.collections.EntryRef), $a:(ref $kotlin.String)=(ref $kotlin.String), $b:(ref $kotlin.String)=(ref $kotlin.String) (4, 4, 4, 4, 4, 4, 4, 4, 4, 4)
// test.kt:18 $box: $map:(ref $kotlin.collections.HashMap)=(ref $kotlin.collections.HashMap), $IS_INTERFACE_PARAMETER:(ref $kotlin.collections.EntryRef)=(ref $kotlin.collections.EntryRef), $a:(ref $kotlin.String)=(ref $kotlin.String), $b:(ref $kotlin.String)=(ref $kotlin.String) (8)
// test.kt:20 $box: $map:(ref $kotlin.collections.HashMap)=(ref $kotlin.collections.HashMap), $IS_INTERFACE_PARAMETER:(ref $kotlin.collections.EntryRef)=(ref $kotlin.collections.EntryRef), $a:(ref $kotlin.String)=(ref $kotlin.String), $b:(ref $kotlin.String)=(ref $kotlin.String) (1, 1)
