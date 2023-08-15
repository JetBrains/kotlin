// IGNORE_BACKEND: WASM
// FILE: test.kt

fun foo(x: Int): Int {
    when (x) {
        21 ->
            1
        42 ->
            2
        else ->
            3
    }

    val t = when (x) {
        21 ->
            1
        42 ->
            2
        else ->
            3
    }

    return t
}

fun box() {
    foo(21)
    foo(42)
    foo(63)
}

// EXPECTATIONS JVM JVM_IR
// test.kt:27 box
// test.kt:5 foo
// test.kt:7 foo
// test.kt:14 foo
// test.kt:16 foo
// test.kt:14 foo
// test.kt:23 foo
// test.kt:27 box
// test.kt:28 box
// test.kt:5 foo
// test.kt:9 foo
// test.kt:14 foo
// test.kt:18 foo
// test.kt:14 foo
// test.kt:23 foo
// test.kt:28 box
// test.kt:29 box
// test.kt:5 foo
// test.kt:11 foo
// test.kt:14 foo
// test.kt:20 foo
// test.kt:14 foo
// test.kt:23 foo
// test.kt:29 box
// test.kt:30 box

// EXPECTATIONS JS_IR
// test.kt:27 box
// test.kt:5 foo
// test.kt:14 foo
// test.kt:16 foo
// test.kt:23 foo
// test.kt:28 box
// test.kt:5 foo
// test.kt:14 foo
// test.kt:18 foo
// test.kt:23 foo
// test.kt:29 box
// test.kt:5 foo
// test.kt:14 foo
// test.kt:20 foo
// test.kt:23 foo
// test.kt:30 box
