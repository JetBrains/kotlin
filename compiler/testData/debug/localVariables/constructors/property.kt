// IGNORE_BACKEND_K2_LIGHT_TREE: JVM_IR
//   Reason: KT-56755
// FILE: test.kt
class F(val a: String)

fun box() {
    F("foo")
}

// EXPECTATIONS JVM JVM_IR
// test.kt:7 box:
// test.kt:4 <init>: a:java.lang.String="foo":java.lang.String
// test.kt:7 box:
// test.kt:8 box:

// EXPECTATIONS JS_IR
// test.kt:7 box:
// test.kt:4 <init>: a="foo":kotlin.String
// test.kt:4 <init>: a="foo":kotlin.String
// test.kt:8 box:
