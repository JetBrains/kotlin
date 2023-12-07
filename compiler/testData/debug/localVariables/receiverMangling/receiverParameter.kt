// FILE: test.kt
fun String.test() {

}

fun box() {
    "OK".test()
}

// EXPECTATIONS JVM_IR
// test.kt:7 box:
// test.kt:4 test: $this$test:java.lang.String="OK":java.lang.String
// test.kt:8 box:

// EXPECTATIONS JS_IR
// test.kt:7 box:
// test.kt:4 test: <this>="OK":kotlin.String
// test.kt:8 box:
