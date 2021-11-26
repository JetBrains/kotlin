// FILE: test.kt

fun box() {
    "OK"
    fun bar() = "OK"
    "OK"
    bar()
    "OK"
}

// EXPECTATIONS
// test.kt:4 box
// EXPECTATIONS JVM
// test.kt:5 box
// EXPECTATIONS
// test.kt:6 box
// test.kt:7 box
// EXPECTATIONS JVM
// test.kt:5 invoke
// EXPECTATIONS JVM_IR
// test.kt:5 box$bar
// EXPECTATIONS
// test.kt:7 box
// test.kt:8 box
// test.kt:9 box