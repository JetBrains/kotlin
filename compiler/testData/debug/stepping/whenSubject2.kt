// IGNORE_BACKEND: WASM
// FILE: test.kt

fun foo(x: Int) {
    when
        (x)
    {
        21 -> foo(42)
        42 -> foo(63)
        else -> 1
    }

    val t = when
        (x)
    {
        21 -> foo(42)
        42 -> foo(63)
        else -> 2
    }
}

fun box() {
    foo(21)
}

// EXPECTATIONS JVM_IR
// test.kt:23 box
// test.kt:6 foo
// test.kt:5 foo
// test.kt:8 foo
// test.kt:6 foo
// test.kt:5 foo
// test.kt:9 foo
// test.kt:6 foo
// test.kt:5 foo
// test.kt:10 foo
// test.kt:14 foo
// test.kt:13 foo
// test.kt:18 foo
// test.kt:13 foo
// test.kt:20 foo
// test.kt:9 foo
// test.kt:14 foo
// test.kt:13 foo
// test.kt:17 foo
// test.kt:6 foo
// test.kt:5 foo
// test.kt:10 foo
// test.kt:14 foo
// test.kt:13 foo
// test.kt:18 foo
// test.kt:13 foo
// test.kt:20 foo
// test.kt:17 foo
// test.kt:13 foo
// test.kt:20 foo
// test.kt:8 foo
// test.kt:14 foo
// test.kt:13 foo
// test.kt:16 foo
// test.kt:6 foo
// test.kt:5 foo
// test.kt:9 foo
// test.kt:6 foo
// test.kt:5 foo
// test.kt:10 foo
// test.kt:14 foo
// test.kt:13 foo
// test.kt:18 foo
// test.kt:13 foo
// test.kt:20 foo
// test.kt:9 foo
// test.kt:14 foo
// test.kt:13 foo
// test.kt:17 foo
// test.kt:6 foo
// test.kt:5 foo
// test.kt:10 foo
// test.kt:14 foo
// test.kt:13 foo
// test.kt:18 foo
// test.kt:13 foo
// test.kt:20 foo
// test.kt:17 foo
// test.kt:13 foo
// test.kt:20 foo
// test.kt:16 foo
// test.kt:13 foo
// test.kt:20 foo
// test.kt:24 box

// EXPECTATIONS JS_IR
// test.kt:23 box
// test.kt:5 foo
// test.kt:8 foo
// test.kt:5 foo
// test.kt:9 foo
// test.kt:5 foo
// test.kt:13 foo
// test.kt:18 foo
// test.kt:13 foo
// test.kt:20 foo
// test.kt:13 foo
// test.kt:17 foo
// test.kt:5 foo
// test.kt:13 foo
// test.kt:18 foo
// test.kt:13 foo
// test.kt:20 foo
// test.kt:13 foo
// test.kt:20 foo
// test.kt:13 foo
// test.kt:16 foo
// test.kt:5 foo
// test.kt:9 foo
// test.kt:5 foo
// test.kt:13 foo
// test.kt:18 foo
// test.kt:13 foo
// test.kt:20 foo
// test.kt:13 foo
// test.kt:17 foo
// test.kt:5 foo
// test.kt:13 foo
// test.kt:18 foo
// test.kt:13 foo
// test.kt:20 foo
// test.kt:13 foo
// test.kt:20 foo
// test.kt:13 foo
// test.kt:20 foo
// test.kt:24 box
