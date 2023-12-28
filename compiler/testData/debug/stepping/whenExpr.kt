// IGNORE_BACKEND_K2: WASM
// FILE: test.kt

fun box() {
    val a: Int? = 3
    when (a) {
        1 -> {
            1
        }
        2 -> {
            2
        }
        else -> {
            0
        }
    }
}

// EXPECTATIONS JVM_IR
// test.kt:5 box
// test.kt:6 box
// test.kt:7 box
// test.kt:10 box
// test.kt:14 box
// test.kt:17 box

// EXPECTATIONS JS_IR
// test.kt:5 box
// test.kt:6 box
// test.kt:17 box

// EXPECTATIONS WASM
// test.kt:1 $box
// test.kt:5 $box (18, 18, 18, 18)
// test.kt:6 $box
// test.kt:7 $box
// Runtime.kt:47 $kotlin.wasm.internal.nullableEquals (25, 8, 25, 8)
// Runtime.kt:49 $kotlin.wasm.internal.nullableEquals (25, 30, 37, 30, 4, 25, 30, 37, 30, 4)
// Primitives.kt:1363 $kotlin.Int__equals-impl (42, 42, 24, 48, 42, 42, 24, 48)
// test.kt:10 $box
// test.kt:17 $box
