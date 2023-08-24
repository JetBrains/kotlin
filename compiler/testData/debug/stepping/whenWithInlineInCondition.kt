// IGNORE_BACKEND: WASM
// FILE: test.kt
fun box() {
    val x = value()
    when (x) {
        x0() -> nop()
        x1() -> nop()
        x2() -> nop()
        x3() -> nop()
        else -> nop()
    }

    when (x0() + x1()) {
        x0().rid() -> nop()
        id(x1()) -> nop()
        else -> nop()
    }
}

fun value(): Int = 2
inline fun x0(): Int = 0
inline fun x1(): Int = 1
inline fun x2(): Int = 2
inline fun x3(): Int = 3

inline fun id(x: Int): Int = x
inline fun Int.rid(): Int = this

fun nop() {}

// JVM_IR generates an additional line number for the end of the condition, which is necessary for the correct "step over" behavior.

// EXPECTATIONS JVM JVM_IR
// test.kt:4 box
// test.kt:20 value
// test.kt:4 box
// test.kt:5 box
// test.kt:6 box
// test.kt:21 box
// EXPECTATIONS JVM_IR
// test.kt:6 box
// EXPECTATIONS JVM JVM_IR
// test.kt:7 box
// test.kt:22 box
// EXPECTATIONS JVM_IR
// test.kt:7 box
// EXPECTATIONS JVM JVM_IR
// test.kt:8 box
// test.kt:23 box
// test.kt:8 box
// test.kt:29 nop
// test.kt:8 box
// test.kt:13 box
// test.kt:21 box
// test.kt:13 box
// test.kt:22 box
// test.kt:13 box
// test.kt:14 box
// test.kt:21 box
// test.kt:14 box
// test.kt:27 box
// EXPECTATIONS JVM_IR
// test.kt:14 box
// EXPECTATIONS JVM JVM_IR
// test.kt:15 box
// test.kt:22 box
// test.kt:15 box
// test.kt:26 box
// test.kt:15 box
// test.kt:29 nop
// test.kt:15 box
// test.kt:18 box

// EXPECTATIONS JS_IR
// test.kt:4 box
// test.kt:20 value
// test.kt:6 box
// test.kt:7 box
// test.kt:8 box
// test.kt:8 box
// test.kt:29 nop
// test.kt:21 box
// test.kt:13 box
// test.kt:14 box
// test.kt:15 box
// test.kt:15 box
// test.kt:29 nop
// test.kt:18 box
