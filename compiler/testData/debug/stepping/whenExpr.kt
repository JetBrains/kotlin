
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
// test.kt:5 $box (18, 18)
// test.kt:6 $box
// test.kt:7 $box
// test.kt:10 $box
// test.kt:14 $box
// test.kt:17 $box
