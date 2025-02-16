

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
// test.kt:6 $box (4)
// test.kt:12 $box (12)
// test.kt:13 $box (6, 4)
// test.kt:7 $box (16)
// test.kt:8 $box (5)
// test.kt:14 $box (1)
// test.kt:9 $box (1)
