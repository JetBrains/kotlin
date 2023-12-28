
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

// EXPECTATIONS JVM_IR
// test.kt:6 box
// test.kt:10 test
// test.kt:11 test
// test.kt:12 test
// test.kt:6 box
// test.kt:7 box

// EXPECTATIONS JS_IR
// test.kt:6 box
// test.kt:12 test
// test.kt:7 box

// EXPECTATIONS WASM
// test.kt:1 $box
// test.kt:6 $box (4, 4)
// test.kt:11 $test
// test.kt:12 $test (15, 8)
// test.kt:7 $box
