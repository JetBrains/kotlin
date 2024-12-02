// IGNORE_INLINER: IR
// FILE: test.kt
fun box() {
    if (inlineFun()) {
        nop()
    }

    if (
        inlineFun().rid() &&
        inlineFun()
    ) {
        nop()
    }

    if (
        id(
            inlineFun()
        )
    ) {
        nop()
    }
}

inline fun inlineFun(): Boolean {
    return true
}

inline fun id(x: Boolean): Boolean = x
inline fun Boolean.rid(): Boolean = this

fun nop() {}

// EXPECTATIONS JVM_IR
// test.kt:4 box
// test.kt:25 box
// test.kt:4 box
// test.kt:5 box
// test.kt:31 nop
// test.kt:9 box
// test.kt:25 box
// test.kt:9 box
// test.kt:29 box
// test.kt:9 box
// test.kt:10 box
// test.kt:25 box
// test.kt:10 box
// test.kt:12 box
// test.kt:31 nop
// test.kt:17 box
// test.kt:25 box
// test.kt:17 box
// test.kt:16 box
// test.kt:28 box
// test.kt:16 box
// test.kt:20 box
// test.kt:31 nop
// test.kt:22 box

// EXPECTATIONS JS_IR
// test.kt:5 box
// test.kt:31 nop
// test.kt:25 box
// EXPECTATIONS FIR JS_IR
// test.kt:9 box
// EXPECTATIONS ClassicFrontend JS_IR
// test.kt:8 box
// EXPECTATIONS JS_IR
// test.kt:12 box
// test.kt:31 nop
// test.kt:20 box
// test.kt:31 nop
// test.kt:22 box

// EXPECTATIONS WASM
// test.kt:3 $box (10)
// test.kt:4 $box (8)
// test.kt:24 $box (32)
// test.kt:25 $box (11, 4)
// test.kt:5 $box (8)
// test.kt:31 $nop (10, 12)
// test.kt:9 $box (8, 20)
// test.kt:29 $box (36)
// test.kt:10 $box (8)
// test.kt:12 $box (8)
// test.kt:17 $box (12)
// test.kt:16 $box (8)
// test.kt:28 $box (37)
// test.kt:20 $box (8)
// test.kt:22 $box (1)
