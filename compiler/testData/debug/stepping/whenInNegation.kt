
// FILE: test.kt

fun foo(n: Number) {
    if (!when (n) {
            is Float -> false
            else -> true
        }) {
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
// test.kt:13 $box (8, 8, 4)
// test.kt:5 $foo (15, 8)
// test.kt:6 $foo (12, 12, 24)
// test.kt:10 $foo
// test.kt:14 $box
