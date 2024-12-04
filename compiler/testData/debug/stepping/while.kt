
// FILE: test.kt

fun box() {
    var x = 2
    while (--x > 0) {
        "OK"
    }

    x = 2
    do {
        "OK"
    } while (--x > 0)
}

// EXPECTATIONS JVM_IR
// test.kt:5 box
// test.kt:6 box
// test.kt:7 box
// test.kt:6 box
// test.kt:10 box
// test.kt:12 box
// test.kt:13 box
// test.kt:12 box
// test.kt:13 box
// test.kt:14 box

// EXPECTATIONS JS_IR
// test.kt:5 box
// test.kt:6 box
// test.kt:6 box
// test.kt:6 box
// test.kt:6 box
// test.kt:10 box
// test.kt:13 box
// test.kt:13 box
// test.kt:13 box
// test.kt:13 box
// test.kt:14 box

// EXPECTATIONS WASM
// test.kt:5 $box (12, 4)
// test.kt:6 $box (13, 11, 13, 17, 11, 11, 11, 13, 11, 13, 17, 11, 11, 11)
// test.kt:7 $box (8, 8, 8, 8, 8)
// test.kt:10 $box (8, 4)
// test.kt:12 $box (8, 8, 8, 8, 8, 8)
// test.kt:13 $box (15, 13, 15, 19, 13, 13, 15, 13, 15, 19, 13, 13)
// test.kt:14 $box
