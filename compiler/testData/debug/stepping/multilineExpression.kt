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

// EXPECTATIONS JVM JVM_IR
// test.kt:5 box
// EXPECTATIONS JVM_IR
// test.kt:9 test
// test.kt:10 test
// EXPECTATIONS JVM JVM_IR
// test.kt:11 test
// test.kt:9 test
// test.kt:5 box
// test.kt:6 box

// EXPECTATIONS JS_IR
// test.kt:5 box
// test.kt:9 test
// test.kt:6 box