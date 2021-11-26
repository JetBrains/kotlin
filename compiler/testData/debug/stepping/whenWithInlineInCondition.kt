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

// EXPECTATIONS
// test.kt:3 box
// test.kt:19 value
// test.kt:3 box
// test.kt:4 box
// test.kt:5 box
// test.kt:20 box
// EXPECTATIONS JVM_IR
// test.kt:5 box
// EXPECTATIONS
// test.kt:6 box
// test.kt:21 box
// EXPECTATIONS JVM_IR
// test.kt:6 box
// EXPECTATIONS
// test.kt:7 box
// test.kt:22 box
// test.kt:7 box
// test.kt:28 nop
// test.kt:7 box
// test.kt:12 box
// test.kt:20 box
// test.kt:12 box
// test.kt:21 box
// test.kt:12 box
// test.kt:13 box
// test.kt:20 box
// test.kt:13 box
// test.kt:26 box
// EXPECTATIONS JVM_IR
// test.kt:13 box
// EXPECTATIONS
// test.kt:14 box
// test.kt:21 box
// test.kt:14 box
// test.kt:25 box
// test.kt:14 box
// test.kt:28 nop
// test.kt:14 box
// test.kt:17 box
