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

// LINENUMBERS
// test.kt:4 box
// test.kt:10 box
// test.kt:11 box
// test.kt:5 box
// test.kt:12 box
// test.kt:7 box
