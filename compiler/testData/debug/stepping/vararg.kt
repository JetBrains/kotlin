// WITH_STDLIB
// FILE: test.kt

val array = intArrayOf(1, 2, 3)

fun sum(vararg v: Int): Int {
    return v.sum()
}

fun box(): String {
    sum(
        *
        array
    )
    sum(v = array)
    sum(1, 2, 3, 4)
    sum()
    return "OK"
}

// EXPECTATIONS JVM_IR
// test.kt:13 box
// test.kt:12 box
// test.kt:11 box
// test.kt:7 sum
// test.kt:11 box
// test.kt:15 box
// test.kt:7 sum
// test.kt:15 box
// test.kt:16 box
// test.kt:7 sum
// test.kt:16 box
// test.kt:17 box
// test.kt:7 sum
// test.kt:17 box
// test.kt:18 box

// EXPECTATIONS NATIVE
// test.kt:1 box
// test.kt:13 box
// test.kt:4 <get-array>
// test.kt:1 <get-array>
// test.kt:4 <get-array>
// test.kt:13 box
// test.kt:12 box
// test.kt:12 box
// test.kt:12 box
// test.kt:12 box
// test.kt:11 box
// test.kt:6 sum
// test.kt:1 sum
// test.kt:7 sum
// test.kt:7 sum
// test.kt:8 sum
// test.kt:11 box
// test.kt:15 box
// test.kt:4 <get-array>
// test.kt:1 <get-array>
// test.kt:4 <get-array>
// test.kt:15 box
// test.kt:15 box
// test.kt:15 box
// test.kt:15 box
// test.kt:6 sum
// test.kt:1 sum
// test.kt:7 sum
// test.kt:7 sum
// test.kt:8 sum
// test.kt:15 box
// test.kt:16 box
// test.kt:16 box
// test.kt:6 sum
// test.kt:1 sum
// test.kt:7 sum
// test.kt:7 sum
// test.kt:8 sum
// test.kt:16 box
// test.kt:17 box
// test.kt:17 box
// test.kt:6 sum
// test.kt:1 sum
// test.kt:7 sum
// test.kt:7 sum
// test.kt:8 sum
// test.kt:17 box
// test.kt:18 box
// test.kt:19 box

// EXPECTATIONS JS_IR
// test.kt:4 <init properties test.kt>
// test.kt:11 box
// test.kt:4 <get-array>
// test.kt:12 box
// test.kt:7 sum
// test.kt:15 box
// test.kt:4 <get-array>
// test.kt:15 box
// test.kt:7 sum
// test.kt:16 box
// test.kt:7 sum
// test.kt:17 box
// test.kt:7 sum
// test.kt:18 box

// EXPECTATIONS WASM
// test.kt:13 $box (8)
// test.kt:12 $box (8)
// test.kt:11 $box (4)
// test.kt:7 $sum (11, 13, 4)
// test.kt:11 $box (4)
// test.kt:15 $box (12, 8, 4)
// test.kt:7 $sum (11, 13, 4)
// test.kt:15 $box (4)
// test.kt:16 $box (8, 4)
// test.kt:7 $sum (11, 13, 4)
// test.kt:16 $box (4)
// test.kt:17 $box (4)
// test.kt:7 $sum (11, 13, 4)
// test.kt:17 $box (4)
// test.kt:18 $box (11, 4)
