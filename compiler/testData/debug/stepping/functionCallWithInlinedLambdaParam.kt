

// FILE: test.kt

fun box() {
    foo({
            val a = 1
        })

    foo() {
        val a = 1
    }
}

inline fun foo(f: () -> Unit) {
    val a = 1
    f()
}

// EXPECTATIONS JVM_IR
// test.kt:6 box
// test.kt:16 box
// test.kt:17 box
// test.kt:7 box
// test.kt:8 box
// test.kt:17 box
// test.kt:18 box
// test.kt:10 box
// test.kt:16 box
// test.kt:17 box
// test.kt:11 box
// test.kt:12 box
// test.kt:17 box
// test.kt:18 box
// test.kt:13 box

// EXPECTATIONS JS_IR
// test.kt:16 box
// test.kt:7 box
// test.kt:16 box
// test.kt:11 box
// test.kt:13 box

// EXPECTATIONS WASM
// test.kt:5 $box (10)
// test.kt:6 $box (4)
// test.kt:15 $box (30)
// test.kt:16 $box (12)
// test.kt:17 $box (4)
// test.kt:7 $box (12, 20, 21)
// test.kt:18 $box (1)
// test.kt:10 $box (4)
// test.kt:11 $box (8, 16, 17)
// test.kt:13 $box (1)
