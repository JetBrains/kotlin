// IGNORE_BACKEND: JS_IR
// FILE: test.kt

fun box() {
    "OK"
    fun bar() = "OK"
    "OK"
    bar()
    "OK"
}

// EXPECTATIONS
// test.kt:5 box
// EXPECTATIONS JVM
// test.kt:6 box
// EXPECTATIONS
// test.kt:7 box
// test.kt:8 box
// EXPECTATIONS JVM
// test.kt:6 invoke
// EXPECTATIONS JVM_IR
// test.kt:6 box$bar
// EXPECTATIONS
// test.kt:8 box
// test.kt:9 box
// test.kt:10 box
