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
// test.kt:3 box
// test.kt:24 box
// test.kt:3 box
// test.kt:4 box
// test.kt:30 nop
// test.kt:8 box
// test.kt:24 box
// test.kt:8 box
// test.kt:28 box
// test.kt:8 box
// test.kt:9 box
// test.kt:24 box
// test.kt:9 box
// test.kt:11 box
// test.kt:30 nop
// test.kt:16 box
// test.kt:24 box
// test.kt:16 box
// test.kt:15 box
// test.kt:27 box
// test.kt:15 box
// test.kt:19 box
// test.kt:30 nop
// test.kt:21 box

// EXPECTATIONS JS_IR
// test.kt:4 box
// test.kt:30 nop
// test.kt:24 box
// test.kt:7 box
// test.kt:11 box
// test.kt:30 nop
// test.kt:19 box
// test.kt:30 nop
// test.kt:21 box
