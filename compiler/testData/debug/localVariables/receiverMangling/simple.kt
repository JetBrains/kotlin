

// FILE: test.kt
fun String.foo(a: Int) {}

fun box() {
    "OK".foo(42)
}

// EXPECTATIONS JVM_IR
// test.kt:7 box:
// test.kt:4 foo: $this$foo:java.lang.String="OK":java.lang.String, a:int=42:int
// test.kt:8 box:

// EXPECTATIONS JS_IR
// test.kt:7 box:
// test.kt:4 foo: <this>="OK":kotlin.String, a=42:number
// test.kt:8 box:
