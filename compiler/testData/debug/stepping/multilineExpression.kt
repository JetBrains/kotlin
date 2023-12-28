
// FILE: test.kt
// KT-17753

fun box() {
    test(true, true, true)
}

fun test(a: Boolean, b: Boolean, c: Boolean): Boolean {
    return a
            && b
            && c
}

// EXPECTATIONS JVM_IR
// test.kt:6 box
// test.kt:10 test
// test.kt:11 test
// test.kt:12 test
// test.kt:10 test
// test.kt:6 box
// test.kt:7 box

// EXPECTATIONS JS_IR
// test.kt:6 box
// test.kt:10 test
// test.kt:7 box

// EXPECTATIONS WASM
// test.kt:1 $box
// test.kt:6 $box (9, 15, 21, 4, 4)
// test.kt:10 $test (11, 4)
// test.kt:11 $test
// test.kt:12 $test
// test.kt:7 $box
