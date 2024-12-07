
// FILE: test.kt

fun box() {
    {
        "OK"
    }()
}

// EXPECTATIONS JVM_IR
// test.kt:6 box
// test.kt:8 box

// EXPECTATIONS JS_IR
// test.kt:5 box
// test.kt:6 box$lambda
// test.kt:8 box

// EXPECTATIONS WASM
// test.kt:5 $box (4, 4, 4)
// test.kt:6 $box$lambda.invoke (8, 8, 8, 8, 12)
// test.kt:8 $box
