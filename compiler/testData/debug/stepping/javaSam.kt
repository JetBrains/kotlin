// TARGET_BACKEND: JVM

// FILE: test.kt

fun box() {
    Runnable {
    }.run()
}

// EXPECTATIONS JVM
// test.kt:6 box
// test.kt:7 box
// test.kt:8 box
