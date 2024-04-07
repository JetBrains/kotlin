

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

// EXPECTATIONS JVM_IR
// test.kt:6 box
// test.kt:12 box
// test.kt:13 box
// test.kt:7 box
// test.kt:8 box
// test.kt:13 box
// test.kt:14 box
// test.kt:9 box

// EXPECTATIONS JS_IR
// test.kt:12 box
// test.kt:7 box
// test.kt:9 box

// EXPECTATIONS WASM
// test.kt:6 $box
// test.kt:12 $box (12, 12, 12, 12, 4)
// test.kt:13 $box (4, 6)
// test.kt:7 $box (16, 16, 16, 16, 8)
// test.kt:9 $box
