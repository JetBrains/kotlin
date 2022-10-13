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

// The JVM IR backend does not generate line number information for the
// declaration of local variables without initializer. Stepping through
// those does not seem useful. This is consistent with javac behavior
// as well. The JVM backend does generate these line numbers.

// EXPECTATIONS JVM
// test.kt:4 box
// test.kt:5 box
// test.kt:6 box
// EXPECTATIONS JVM JVM_IR
// test.kt:7 box
// test.kt:8 box
// test.kt:9 box
// test.kt:10 box
// test.kt:12 box

// EXPECTATIONS JS_IR
// test.kt:7 box
// test.kt:8 box
// test.kt:9 box
// test.kt:10 box
// test.kt:12 box