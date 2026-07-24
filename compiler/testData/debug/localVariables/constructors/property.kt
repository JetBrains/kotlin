// FILE: test.kt
class F(val a: String)

fun box() {
    F("foo")
}

// EXPECTATIONS JVM_IR
// test.kt:5 box:
// test.kt:2 <init>: a:java.lang.String="foo":java.lang.String
// test.kt:5 box:
// test.kt:6 box:

// EXPECTATIONS JS_IR
// test.kt:5 box:
// test.kt:2 <init>: a="foo":kotlin.String
// test.kt:2 <init>: a="foo":kotlin.String
// test.kt:6 box:

// EXPECTATIONS WASM
// test.kt:5 $box: (4, 6, 4)
// test.kt:2 $F.<init>: $<this>:(ref $F)=(ref $F), $a:(ref $kotlin.String)=(ref $kotlin.String) (8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 22, 22, 22)
// test.kt:5 $box: (4)
// test.kt:6 $box: (1)
