// FILE: test.kt

fun box() {
    lookAtMe {
        val c = "c"
    }
}

inline fun lookAtMe(f: (String) -> Unit) {
    val a = "a"
    f(a)
}

// EXPECTATIONS
// test.kt:4 box
// test.kt:10 box
// test.kt:11 box
// test.kt:5 box
// test.kt:6 box
// test.kt:12 box
// test.kt:7 box