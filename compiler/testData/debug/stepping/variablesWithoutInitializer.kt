
// FILE: test.kt

fun box() {
    var x: String
    var y: Int
    var z: Boolean
    z = false
    y = 42
    if (!z) {
        x = y.toString()
    }
}

// EXPECTATIONS JVM_IR
// test.kt:5 box
// test.kt:6 box
// test.kt:7 box
// test.kt:8 box
// test.kt:9 box
// test.kt:10 box
// test.kt:11 box
// test.kt:13 box

// EXPECTATIONS JS_IR
// test.kt:8 box
// test.kt:9 box
// test.kt:10 box
// test.kt:11 box
// test.kt:13 box

// EXPECTATIONS WASM
// test.kt:8 $box (8, 4)
// test.kt:9 $box (8, 4)
// test.kt:10 $box (9, 8)
// test.kt:11 $box (12, 14, 8)
// test.kt:13 $box
