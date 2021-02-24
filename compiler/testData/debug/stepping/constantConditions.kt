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

// LINENUMBERS
// test.kt:5 box
// LINENUMBERS JVM_IR
// test.kt:9 test
// LINENUMBERS
// test.kt:10 test
// test.kt:11 test
// test.kt:5 box
// test.kt:6 box
