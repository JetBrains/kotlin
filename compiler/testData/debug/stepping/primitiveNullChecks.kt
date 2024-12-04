
// FILE: test.kt

fun box(): String {
    42!!
    42.toLong()!!
    return "OK"!!
}

// EXPECTATIONS JVM_IR
// test.kt:5 box
// test.kt:6 box
// test.kt:7 box

// EXPECTATIONS JS_IR
// test.kt:5 box
// test.kt:6 box
// test.kt:6 box
// test.kt:7 box

// EXPECTATIONS WASM
// test.kt:5 $box
// test.kt:6 $box
// test.kt:7 $box (11, 11, 4)
