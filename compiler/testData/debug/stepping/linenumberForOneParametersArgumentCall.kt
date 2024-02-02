

// FILE: test.kt

fun box() {
    lookAtMe {
        val c = "c"
    }
}

inline fun lookAtMe(f: (String) -> Unit) {
    val a = "a"
    f(a)
}

// EXPECTATIONS JVM_IR
// test.kt:6 box
// test.kt:12 box
// test.kt:13 box
// test.kt:7 box
// test.kt:8 box
// test.kt:13 box
// test.kt:14 box
// test.kt:9 box

// EXPECTATIONS JS_IR
// test.kt:12 box
// test.kt:7 box
// test.kt:9 box

// EXPECTATIONS WASM
// test.kt:1 $box
// test.kt:6 $box
// test.kt:12 $box (12, 12, 12, 12, 4)
// String.kt:141 $kotlin.stringLiteral (17, 28, 17, 17, 28, 17)
// Array.kt:59 $kotlin.Array.get (19, 26, 34, 8, 19, 26, 34, 8)
// ThrowHelpers.kt:29 $kotlin.wasm.internal.rangeCheck (6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19)
// ThrowHelpers.kt:30 $kotlin.wasm.internal.rangeCheck (1, 1, 1, 1)
// Array.kt:60 $kotlin.Array.get (15, 27, 23, 8, 15, 27, 23, 8)
// String.kt:142 $kotlin.stringLiteral (8, 8)
// String.kt:146 $kotlin.stringLiteral (47, 61, 16, 4, 47, 61, 16, 4)
// String.kt:147 $kotlin.stringLiteral (20, 20, 20, 20, 27, 33, 41, 20, 4, 20, 20, 20, 20, 27, 33, 41, 20, 4)
// String.kt:148 $kotlin.stringLiteral (4, 15, 25, 4, 4, 15, 25, 4)
// Array.kt:74 $kotlin.Array.set (19, 26, 34, 8, 19, 26, 34, 8)
// Array.kt:75 $kotlin.Array.set (8, 20, 27, 16, 8, 20, 27, 16)
// Array.kt:76 $kotlin.Array.set (5, 5)
// String.kt:149 $kotlin.stringLiteral (11, 4, 11, 4)
// test.kt:13 $box (4, 6)
// test.kt:7 $box (16, 16, 16, 16, 8)
// test.kt:9 $box
