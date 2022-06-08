// IGNORE_BACKEND: JS_IR
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

// EXPECTATIONS JVM JVM_IR
// test.kt:5 box
// test.kt:11 box
// test.kt:12 box
// test.kt:6 box
// test.kt:7 box
// test.kt:12 box
// test.kt:13 box
// test.kt:8 box

// EXPECTATIONS JS_IR
