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
// test.kt:26 box
// test.kt:4 foo
// test.kt:6 foo
// test.kt:13 foo
// test.kt:15 foo
// test.kt:13 foo
// test.kt:22 foo
// test.kt:26 box
// test.kt:27 box
// test.kt:4 foo
// test.kt:8 foo
// test.kt:13 foo
// test.kt:17 foo
// test.kt:13 foo
// test.kt:22 foo
// test.kt:27 box
// test.kt:28 box
// test.kt:4 foo
// test.kt:10 foo
// test.kt:13 foo
// test.kt:19 foo
// test.kt:13 foo
// test.kt:22 foo
// test.kt:28 box
// test.kt:29 box

// EXPECTATIONS JS_IR
// test.kt:26 box
// test.kt:4 foo
// test.kt:13 foo
// test.kt:15 foo
// test.kt:22 foo
// test.kt:27 box
// test.kt:4 foo
// test.kt:13 foo
// test.kt:17 foo
// test.kt:22 foo
// test.kt:28 box
// test.kt:4 foo
// test.kt:13 foo
// test.kt:19 foo
// test.kt:22 foo
// test.kt:29 box
