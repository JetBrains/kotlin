// IGNORE_BACKEND: WASM
// FILE: test.kt
// KT-22488

fun box() {
    test()
}

fun test(): Long {
    if (1 == 1 &&
        2 == 2) {
        return 0
    }

    return 1
}

// EXPECTATIONS JVM JVM_IR
// test.kt:6 box
// EXPECTATIONS JVM_IR
// test.kt:10 test
// EXPECTATIONS JVM JVM_IR
// test.kt:11 test
// test.kt:12 test
// test.kt:6 box
// test.kt:7 box

// EXPECTATIONS JS_IR
// test.kt:6 box
// test.kt:12 test
// test.kt:7 box