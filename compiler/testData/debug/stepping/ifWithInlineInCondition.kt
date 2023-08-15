// IGNORE_BACKEND: WASM
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

// EXPECTATIONS JVM JVM_IR
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
// test.kt:8 box
// test.kt:12 box
// test.kt:31 nop
// test.kt:20 box
// test.kt:31 nop
// test.kt:22 box
