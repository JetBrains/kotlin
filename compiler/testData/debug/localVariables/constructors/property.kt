// FILE: test.kt
class F(val a: String)

fun box() {
    F("foo")
}

// EXPECTATIONS JVM JVM_IR
// test.kt:5 box:
// test.kt:2 <init>: a:java.lang.String="foo":java.lang.String
// test.kt:5 box:
// test.kt:6 box:

// EXPECTATIONS JS_IR
// test.kt:5 box:
// test.kt:2 <init>: a="foo":kotlin.String
// test.kt:2 <init>: a="foo":kotlin.String
// test.kt:6 box:
