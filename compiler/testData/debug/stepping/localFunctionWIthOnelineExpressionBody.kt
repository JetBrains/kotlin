

// FILE: test.kt

fun box() {
    "OK"
    fun bar() = "OK"
    "OK"
    bar()
    "OK"
}

// EXPECTATIONS JVM
// test.kt:6 box
// test.kt:8 box
// test.kt:9 box
// test.kt:7 box$bar
// test.kt:9 box
// test.kt:10 box
// test.kt:11 box

// EXPECTATIONS NATIVE
// test.kt:9 box
// test.kt:7 box$bar
// test.kt:11 box

// EXPECTATIONS JS_IR JS_IR_ES6
// test.kt:9 box
// test.kt:7 box$bar
// test.kt:11 box

// EXPECTATIONS WASM_JS
// test.kt:6 $box (4)
// test.kt:8 $box (4)
// test.kt:9 $box (4)
// test.kt:7 $bar (16, 20)
// test.kt:9 $box (4)
// test.kt:10 $box (4)
// test.kt:11 $box (1)
