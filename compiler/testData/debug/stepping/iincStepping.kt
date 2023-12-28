

// FILE: test.kt
fun box() {
    var i = 0
    ++i
    i += 1
    i +=
        1
    i -= 1
    i -=
        1
    i = i + 1
    i =
        i + 1
    i =
        i +
            1
}

// EXPECTATIONS JVM_IR
// test.kt:5 box
// test.kt:6 box
// test.kt:7 box
// test.kt:8 box
// test.kt:9 box
// test.kt:8 box
// test.kt:10 box
// test.kt:11 box
// test.kt:12 box
// test.kt:11 box
// test.kt:13 box
// test.kt:15 box
// test.kt:14 box
// test.kt:17 box
// test.kt:18 box
// test.kt:17 box
// test.kt:16 box
// test.kt:19 box

// EXPECTATIONS JS_IR
// test.kt:5 box
// test.kt:6 box
// test.kt:7 box
// test.kt:8 box
// test.kt:10 box
// test.kt:11 box
// test.kt:13 box
// test.kt:15 box
// test.kt:17 box
// test.kt:19 box

// EXPECTATIONS WASM
// test.kt:1 $box
// test.kt:5 $box (12, 4)
// test.kt:6 $box (4, 6)
// Primitives.kt:2 $box
// Primitives.kt:1150 $box (8, 15, 8, 16)
// test.kt:7 $box (4, 9, 4, 4)
// test.kt:8 $box (4, 4, 4)
// test.kt:9 $box
// test.kt:10 $box (4, 9, 4, 4)
// test.kt:11 $box (4, 4, 4)
// test.kt:12 $box
// test.kt:13 $box (8, 12, 8, 4)
// test.kt:15 $box (8, 12, 8)
// test.kt:14 $box
// test.kt:17 $box (8, 8)
// test.kt:18 $box
// test.kt:16 $box
// test.kt:19 $box
