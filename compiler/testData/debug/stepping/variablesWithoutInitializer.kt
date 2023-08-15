// IGNORE_BACKEND: WASM
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

// The JVM IR backend does generate line number information for the
// declaration of local variables without initializer.
// Stepping through those is useful for breakpoinnts.
// The JVM backend does generate these line numbers as well.

// EXPECTATIONS JVM JVM_IR
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