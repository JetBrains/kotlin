// WITH_STDLIB

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

// EXPECTATIONS JVM_IR
// test.kt:5 box:
// test.kt:11 box: map:java.util.Map=java.util.Collections$SingletonMap
// test.kt:9 box: map:java.util.Map=java.util.Collections$SingletonMap
// test.kt:14 box: map:java.util.Map=java.util.Collections$SingletonMap, e:java.util.Map$Entry=java.util.AbstractMap$SimpleImmutableEntry
// test.kt:11 box: map:java.util.Map=java.util.Collections$SingletonMap
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

// EXPECTATIONS WASM
// test.kt:5 $box: $map:(ref null $kotlin.Any)=null, $e:(ref null $kotlin.Any)=null, $IS_INTERFACE_PARAMETER:(ref null $kotlin.Any)=null (41, 41, 41, 48, 48, 48, 41, 35)
// test.kt:11 $box: $map:(ref $kotlin.collections.HashMap)=(ref $kotlin.collections.HashMap), $e:(ref null $kotlin.Any)=null, $IS_INTERFACE_PARAMETER:(ref $kotlin.collections.EntryRef)=(ref $kotlin.collections.EntryRef) (4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4)
// test.kt:14 $box: $map:(ref $kotlin.collections.HashMap)=(ref $kotlin.collections.HashMap), $e:(ref $kotlin.collections.EntryRef)=(ref $kotlin.collections.EntryRef), $IS_INTERFACE_PARAMETER:(ref $kotlin.collections.EntryRef)=(ref $kotlin.collections.EntryRef) (8, 8, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 16, 16, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 8, 8, 8)
// test.kt:11 $box: $map:(ref $kotlin.collections.HashMap)=(ref $kotlin.collections.HashMap), $e:(ref $kotlin.collections.EntryRef)=(ref $kotlin.collections.EntryRef), $IS_INTERFACE_PARAMETER:(ref $kotlin.collections.EntryRef)=(ref $kotlin.collections.EntryRef) (4, 4, 4, 4, 4, 4, 4, 4, 4, 4)
// test.kt:14 $box: $map:(ref $kotlin.collections.HashMap)=(ref $kotlin.collections.HashMap), $e:(ref $kotlin.collections.EntryRef)=(ref $kotlin.collections.EntryRef), $IS_INTERFACE_PARAMETER:(ref $kotlin.collections.EntryRef)=(ref $kotlin.collections.EntryRef) (8)
// test.kt:16 $box: $map:(ref $kotlin.collections.HashMap)=(ref $kotlin.collections.HashMap), $e:(ref $kotlin.collections.EntryRef)=(ref $kotlin.collections.EntryRef), $IS_INTERFACE_PARAMETER:(ref $kotlin.collections.EntryRef)=(ref $kotlin.collections.EntryRef) (1, 1)
