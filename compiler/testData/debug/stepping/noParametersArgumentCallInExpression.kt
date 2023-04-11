
// FILE: test.kt

fun box() {
    lookAtMe {
        42
    }
}

inline fun lookAtMe(f: () -> Int) {
    val a = 21
    a + f()
}

// EXPECTATIONS JVM JVM_IR
// test.kt:5 box
// test.kt:11 box
// test.kt:12 box
// test.kt:6 box
// test.kt:12 box
// test.kt:13 box
// test.kt:8 box

// EXPECTATIONS JS_IR
// test.kt:11 box
// test.kt:12 box
// test.kt:8 box
