
// FILE: test.kt

fun foo(n: Any, other: Any) {
    if (other == when (n) {
            is Int -> n
            else -> 1.0f
        }) {
    }
    if (when (n) {
            is Int -> n
            else -> 1.0f
        } == other) {
    }
    if (other != when (n) {
            is Int -> n
            else -> 1.0f
        }) {
    }
    if (when (n) {
            is Int -> n
            else -> 1.0f
        } != other) {
    }
}

fun box() {
    foo(2, Any())
}

// EXPECTATIONS JVM_IR
// test.kt:28 box
// test.kt:5 foo
// test.kt:6 foo
// test.kt:5 foo
// test.kt:10 foo
// test.kt:11 foo
// test.kt:13 foo
// test.kt:10 foo
// test.kt:15 foo
// test.kt:16 foo
// test.kt:15 foo
// test.kt:20 foo
// test.kt:21 foo
// test.kt:23 foo
// test.kt:20 foo
// test.kt:25 foo
// test.kt:29 box

// EXPECTATIONS JS_IR
// test.kt:28 box
// test.kt:6 foo
// test.kt:6 foo
// test.kt:5 foo
// test.kt:11 foo
// test.kt:11 foo
// test.kt:10 foo
// test.kt:16 foo
// test.kt:16 foo
// test.kt:15 foo
// test.kt:21 foo
// test.kt:21 foo
// test.kt:20 foo
// test.kt:25 foo
// test.kt:29 box

// EXPECTATIONS WASM
// test.kt:28 $box (8, 8, 11, 4)
// test.kt:5 $foo (8, 23, 8, 8, 8, 8)
// test.kt:6 $foo (12, 12, 22)
// test.kt:10 $foo (14, 8, 8, 8, 8, 8)
// test.kt:11 $foo (12, 12, 22)
// test.kt:13 $foo
// test.kt:15 $foo (8, 23, 8, 8, 8, 8, 8)
// test.kt:16 $foo (12, 12, 22)
// test.kt:20 $foo (14, 8, 8, 8, 8, 8, 8)
// test.kt:21 $foo (12, 12, 22)
// test.kt:23 $foo
// test.kt:25 $foo
// test.kt:29 $box
