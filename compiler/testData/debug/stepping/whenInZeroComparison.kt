
// FILE: test.kt

fun foo(n: Number) {
    if (when (n) {
            is Float -> 1
            else -> 0
        } == 0) {
    }
}

fun box() {
    foo(2.0f)
}

// EXPECTATIONS JVM_IR
// test.kt:13 box
// test.kt:5 foo
// test.kt:6 foo
// test.kt:5 foo
// test.kt:10 foo
// test.kt:14 box

// EXPECTATIONS JS_IR
// test.kt:13 box
// test.kt:6 foo
// test.kt:6 foo
// test.kt:10 foo
// test.kt:14 box

// EXPECTATIONS WASM
// test.kt:13 $box (8, 8, 8, 8, 4)
// test.kt:5 $foo (14, 8)
// test.kt:6 $foo (12, 24)
// test.kt:8 $foo
// test.kt:10 $foo
// test.kt:14 $box
