// IGNORE_BACKEND: JS_IR
// FILE: test.kt

fun box() {
    "OK"
    fun bar() {
        "OK"
    }
    "OK"
    bar()
    "OK"
}

// EXPECTATIONS
// test.kt:4 box
// EXPECTATIONS JVM
// test.kt:5 box
// EXPECTATIONS
// test.kt:8 box
// test.kt:9 box
// EXPECTATIONS JVM
// test.kt:6 invoke
// test.kt:7 invoke
// EXPECTATIONS JVM_IR
// test.kt:6 box$bar
// test.kt:7 box$bar
// EXPECTATIONS
// test.kt:10 box
// test.kt:11 box
