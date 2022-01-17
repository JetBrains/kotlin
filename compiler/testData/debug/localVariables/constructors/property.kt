

// FILE: test.kt
class F(val a: String)

fun box() {
    F("foo")
}

// EXPECTATIONS
// test.kt:7 box:
// test.kt:4 <init>: a:java.lang.String="foo":java.lang.String
// test.kt:7 box:
// test.kt:8 box: