
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
// test.kt:5 $box (18)
// test.kt:6 $box (10)
// test.kt:7 $box (8)
// test.kt:10 $box (8)
// test.kt:14 $box (12)
// test.kt:17 $box (1)
