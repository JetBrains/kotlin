
// FILE: test.kt

fun foo(x: Int): Int {
    when {
        x == 21 ->
            1
        x == 42 ->
            2
        else ->
            3
    }

    val t = when {
        x == 21 ->
            1
        x == 42 ->
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

// EXPECTATIONS JVM_IR
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

// EXPECTATIONS WASM
// test.kt:1 $box
// test.kt:27 $box (8, 4, 4)
// test.kt:6 $foo (8, 8, 8)
// test.kt:15 $foo (8, 8, 8)
// test.kt:16 $foo
// test.kt:23 $foo (11, 4, 11, 4, 11, 4)
// test.kt:28 $box (8, 4, 4)
// test.kt:18 $foo
// test.kt:29 $box (8, 4, 4)
// test.kt:20 $foo
// test.kt:30 $box
