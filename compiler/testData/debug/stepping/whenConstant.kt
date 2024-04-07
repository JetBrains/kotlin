
// FILE: test.kt

fun box() {
    when (1) {
        2 ->
            "2"
        3 ->
            "3"
        else ->
            "1"
    }
}

// EXPECTATIONS JVM_IR
// test.kt:5 box
// test.kt:6 box
// test.kt:8 box
// test.kt:11 box
// test.kt:13 box

// EXPECTATIONS JS_IR
// test.kt:5 box
// test.kt:13 box

// EXPECTATIONS WASM
// test.kt:5 $box
// test.kt:6 $box
// test.kt:11 $box (12, 12, 12, 12)
// test.kt:13 $box
