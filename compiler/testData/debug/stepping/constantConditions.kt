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
// test.kt:5 box
// EXPECTATIONS JVM_IR
// test.kt:9 test
// EXPECTATIONS JVM JVM_IR
// test.kt:10 test
// test.kt:11 test
// test.kt:5 box
// test.kt:6 box

// EXPECTATIONS JS_IR
// test.kt:5 box
// test.kt:11 test
// test.kt:6 box