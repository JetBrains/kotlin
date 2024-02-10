

// FILE: test.kt
fun box(){
    test() +
            fail()

    fail() +
                test()
}

inline fun test() : String {
    return "123"
}

fun fail() : String {
    return "fail"
}

// EXPECTATIONS JVM_IR
// test.kt:5 box
// test.kt:13 box
// test.kt:5 box
// test.kt:6 box
// test.kt:17 fail
// test.kt:5 box
// test.kt:8 box
// test.kt:17 fail
// test.kt:8 box
// test.kt:9 box
// test.kt:13 box
// test.kt:8 box
// test.kt:10 box

// EXPECTATIONS JS_IR
// test.kt:13 box
// test.kt:17 fail
// test.kt:8 box
// test.kt:17 fail
// test.kt:8 box
// test.kt:10 box

// EXPECTATIONS WASM
// test.kt:1 $box
// test.kt:5 $box (4, 4, 4)
// test.kt:13 $box (11, 11, 11, 11, 4, 11, 11, 11, 11, 4)
// String.kt:141 $kotlin.stringLiteral (17, 28, 17, 17, 28, 17, 17, 28, 17, 17, 28, 17)
// Array.kt:59 $kotlin.Array.get (19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8)
// ThrowHelpers.kt:29 $kotlin.wasm.internal.rangeCheck (6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19)
// ThrowHelpers.kt:30 $kotlin.wasm.internal.rangeCheck (1, 1, 1, 1, 1, 1)
// Array.kt:60 $kotlin.Array.get (15, 27, 23, 8, 15, 27, 23, 8, 15, 27, 23, 8, 15, 27, 23, 8)
// String.kt:142 $kotlin.stringLiteral (8, 8, 8, 8)
// String.kt:146 $kotlin.stringLiteral (47, 61, 16, 4, 47, 61, 16, 4)
// String.kt:147 $kotlin.stringLiteral (20, 20, 20, 20, 27, 33, 41, 20, 4, 20, 20, 20, 20, 27, 33, 41, 20, 4)
// String.kt:148 $kotlin.stringLiteral (4, 15, 25, 4, 4, 15, 25, 4)
// Array.kt:74 $kotlin.Array.set (19, 26, 34, 8, 19, 26, 34, 8)
// Array.kt:75 $kotlin.Array.set (8, 20, 27, 16, 8, 20, 27, 16)
// Array.kt:76 $kotlin.Array.set (5, 5)
// String.kt:149 $kotlin.stringLiteral (11, 4, 11, 4)
// test.kt:6 $box
// test.kt:17 $fail (11, 11, 11, 11, 4, 11, 11, 11, 11, 4)
// String.kt:28 $kotlin.String.plus (20, 26, 8, 20, 26, 8)
// Library.kt:19 $kotlin.toString (37, 37, 43, 43, 43, 37, 37, 63, 37, 37, 43, 43, 43, 37, 37, 63)
// String.kt:119 $kotlin.String.toString (49, 49)
// String.kt:29 $kotlin.String.plus (15, 15, 15, 15, 22, 28, 33, 42, 48, 28, 62, 56, 15, 8, 15, 15, 15, 15, 22, 28, 33, 42, 48, 28, 62, 56, 15, 8)
// String.kt:63 $kotlin.String.plus (12, 12)
// String.kt:66 $kotlin.String.plus (15, 8, 15, 8)
// test.kt:8 $box (4, 4, 4)
// String.kt:143 $kotlin.stringLiteral (15, 8, 15, 8)
// test.kt:9 $box
// test.kt:10 $box
