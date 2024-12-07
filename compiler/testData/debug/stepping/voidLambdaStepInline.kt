
// FILE: test.kt

fun box(): String {
    run { "O" + "K" }
    run {
        "O" + "K"
    }
    return "OK"
}

// EXPECTATIONS JVM_IR
// test.kt:5 box
// fake.kt:1 box
// test.kt:5 box
// test.kt:6 box
// test.kt:7 box
// test.kt:6 box
// test.kt:9 box

// EXPECTATIONS JS_IR
// test.kt:9 box

// EXPECTATIONS WASM
// test.kt:5 $box (4, 10, 10, 19)
// test.kt:6 $box
// test.kt:7 $box (8, 8, 17)
// test.kt:9 $box (11, 11, 11, 11, 4)
