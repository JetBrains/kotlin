// WITH_STDLIB
// FILE: test.kt

val array = intArrayOf(1, 2, 3)

fun sum(vararg v: Int, p: Int = 1): Int {
    return v.sum() + p
}

fun box(): String {
    sum()
    sum(v = array)
    sum(1)
    sum(v = array, 4)
    sum(
        *
        array,
        p = 4)
    sum(
        *
        array,
        4)
    return "OK"
}

// EXPECTATIONS JVM_IR
// test.kt:11 box
// test.kt:7 sum
// test.kt:11 box
// test.kt:12 box
// test.kt:7 sum
// test.kt:12 box
// test.kt:13 box
// test.kt:7 sum
// test.kt:13 box
// test.kt:14 box
// test.kt:7 sum
// test.kt:14 box
// test.kt:17 box
// test.kt:16 box
// test.kt:18 box
// test.kt:15 box
// test.kt:7 sum
// test.kt:15 box
// test.kt:20 box
// test.kt:21 box
// test.kt:20 box
// test.kt:22 box
// test.kt:20 box
// test.kt:19 box
// test.kt:7 sum
// test.kt:19 box
// test.kt:23 box

// EXPECTATIONS NATIVE
// test.kt:1 box
// test.kt:11 box
// test.kt:11 box
// test.kt:6 sum$default
// test.kt:1 sum$default
// test.kt:6 sum$default
// test.kt:8 sum$default
// test.kt:6 sum$default
// test.kt:6 sum
// test.kt:1 sum
// test.kt:7 sum
// test.kt:7 sum
// test.kt:8 sum
// test.kt:6 sum$default
// test.kt:8 sum$default
// test.kt:11 box
// test.kt:12 box
// test.kt:4 <get-array>
// test.kt:1 <get-array>
// test.kt:4 <get-array>
// test.kt:12 box
// test.kt:12 box
// test.kt:12 box
// test.kt:12 box
// test.kt:6 sum$default
// test.kt:1 sum$default
// test.kt:6 sum$default
// test.kt:8 sum$default
// test.kt:6 sum$default
// test.kt:6 sum
// test.kt:1 sum
// test.kt:7 sum
// test.kt:7 sum
// test.kt:8 sum
// test.kt:6 sum$default
// test.kt:8 sum$default
// test.kt:12 box
// test.kt:13 box
// test.kt:13 box
// test.kt:6 sum$default
// test.kt:1 sum$default
// test.kt:6 sum$default
// test.kt:8 sum$default
// test.kt:6 sum$default
// test.kt:6 sum
// test.kt:1 sum
// test.kt:7 sum
// test.kt:7 sum
// test.kt:8 sum
// test.kt:6 sum$default
// test.kt:8 sum$default
// test.kt:13 box
// test.kt:14 box
// test.kt:4 <get-array>
// test.kt:1 <get-array>
// test.kt:4 <get-array>
// test.kt:14 box
// test.kt:14 box
// test.kt:14 box
// test.kt:14 box
// test.kt:6 sum
// test.kt:1 sum
// test.kt:7 sum
// test.kt:7 sum
// test.kt:8 sum
// test.kt:14 box
// test.kt:17 box
// test.kt:4 <get-array>
// test.kt:1 <get-array>
// test.kt:4 <get-array>
// test.kt:17 box
// test.kt:16 box
// test.kt:16 box
// test.kt:16 box
// test.kt:16 box
// test.kt:15 box
// test.kt:6 sum
// test.kt:1 sum
// test.kt:7 sum
// test.kt:7 sum
// test.kt:8 sum
// test.kt:15 box
// test.kt:21 box
// test.kt:4 <get-array>
// test.kt:1 <get-array>
// test.kt:4 <get-array>
// test.kt:21 box
// test.kt:22 box
// test.kt:20 box
// test.kt:20 box
// test.kt:20 box
// test.kt:20 box
// test.kt:19 box
// test.kt:6 sum$default
// test.kt:1 sum$default
// test.kt:6 sum$default
// test.kt:8 sum$default
// test.kt:6 sum$default
// test.kt:6 sum
// test.kt:1 sum
// test.kt:7 sum
// test.kt:7 sum
// test.kt:8 sum
// test.kt:6 sum$default
// test.kt:8 sum$default
// test.kt:19 box
// test.kt:23 box
// test.kt:24 box

// EXPECTATIONS JS_IR
// test.kt:4 <init properties test.kt>
// test.kt:11 box
// test.kt:7 sum
// test.kt:12 box
// test.kt:4 <get-array>
// test.kt:12 box
// test.kt:7 sum
// test.kt:13 box
// test.kt:7 sum
// test.kt:14 box
// test.kt:4 <get-array>
// test.kt:14 box
// test.kt:7 sum
// test.kt:15 box
// test.kt:4 <get-array>
// test.kt:16 box
// test.kt:7 sum
// test.kt:19 box
// test.kt:4 <get-array>
// test.kt:20 box
// test.kt:19 box
// test.kt:7 sum
// test.kt:23 box

// EXPECTATIONS WASM
// test.kt:11 $box (4)
// test.kt:6 $sum$default (32)
// test.kt:7 $sum (11, 13, 21, 11, 4)
// test.kt:6 $sum$default (32)
// test.kt:11 $box (4)
// test.kt:12 $box (12, 8, 4)
// test.kt:6 $sum$default (32)
// test.kt:7 $sum (11, 13, 21, 11, 4)
// test.kt:6 $sum$default (32)
// test.kt:12 $box (4)
// test.kt:13 $box (8, 4)
// test.kt:6 $sum$default (32)
// test.kt:7 $sum (11, 13, 21, 11, 4)
// test.kt:6 $sum$default (32)
// test.kt:13 $box (4)
// test.kt:14 $box (12, 8, 19, 4)
// test.kt:7 $sum (11, 13, 21, 11, 4)
// test.kt:14 $box (4)
// test.kt:17 $box (8)
// test.kt:16 $box (8)
// test.kt:18 $box (12)
// test.kt:15 $box (4)
// test.kt:7 $sum (11, 13, 21, 11, 4)
// test.kt:15 $box (4)
// test.kt:21 $box (8)
// test.kt:22 $box (8)
// test.kt:20 $box (8)
// test.kt:19 $box (4)
// test.kt:6 $sum$default (32)
// test.kt:7 $sum (11, 13, 21, 11, 4)
// test.kt:6 $sum$default (32)
// test.kt:19 $box (4)
// test.kt:23 $box (11, 4)
