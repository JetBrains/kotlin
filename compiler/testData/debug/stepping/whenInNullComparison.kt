
// FILE: test.kt

fun foo(n: Number) {
    if (when (n) {
            is Float -> null
            else -> 32
        } == null) {
    }
    if (when (n) {
            is Float -> null
            else -> 32
        } != null) {
    }
}

fun box() {
    foo(2.0f)
}

// EXPECTATIONS JVM_IR
// test.kt:18 box
// test.kt:5 foo
// test.kt:6 foo
// test.kt:5 foo
// test.kt:10 foo
// test.kt:11 foo
// test.kt:10 foo
// test.kt:15 foo
// test.kt:19 box

// EXPECTATIONS JS_IR
// test.kt:18 box
// test.kt:6 foo
// test.kt:6 foo
// test.kt:11 foo
// test.kt:11 foo
// test.kt:15 foo
// test.kt:19 box

// EXPECTATIONS WASM
// test.kt:18 $box (8, 8, 4)
// test.kt:5 $foo
// test.kt:6 $foo (12, 24)
// test.kt:10 $foo (14, 8)
// test.kt:11 $foo (12, 24)
// test.kt:15 $foo
// test.kt:19 $box
