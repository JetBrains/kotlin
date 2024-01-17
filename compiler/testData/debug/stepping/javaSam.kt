// TARGET_BACKEND: JVM_IR

// FILE: test.kt

fun box() {
    Runnable {
    }.run()
}

// EXPECTATIONS JVM_IR
// test.kt:6 box
// test.kt:7 box
// test.kt:8 box
