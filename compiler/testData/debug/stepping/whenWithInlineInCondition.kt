
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

// EXPECTATIONS JVM_IR
// test.kt:4 box
// test.kt:20 value
// test.kt:4 box
// test.kt:5 box
// test.kt:6 box
// test.kt:21 box
// test.kt:6 box
// test.kt:7 box
// test.kt:22 box
// test.kt:7 box
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
// test.kt:14 box
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

// EXPECTATIONS WASM
// test.kt:1 $box
// test.kt:4 $box
// test.kt:20 $value (19, 20)
// test.kt:5 $box
// test.kt:6 $box (8, 8)
// test.kt:21 $box (23, 24, 23, 24, 23, 24)
// test.kt:7 $box (8, 8)
// test.kt:22 $box (23, 24, 23, 24, 23, 24)
// test.kt:8 $box (8, 8, 16)
// test.kt:23 $box (23, 24)
// test.kt:29 $nop (12, 12)
// test.kt:13 $box (10, 17, 10)
// test.kt:14 $box (8, 13, 8, 8)
// test.kt:27 $box (28, 32)
// test.kt:15 $box (8, 11, 8, 20)
// test.kt:26 $box (29, 30)
// test.kt:18 $box
