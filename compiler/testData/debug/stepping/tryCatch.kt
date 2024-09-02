
// FILE: test.kt

fun foo(shouldThrow: Boolean) {
    try {
        if (shouldThrow) throw Exception()
    } catch (e: Exception) {
        "OK"
    }
    "OK"
}

fun box() {
    foo(false)
    foo(true)
}

// EXPECTATIONS JVM_IR
// test.kt:14 box
// test.kt:5 foo
// test.kt:6 foo
// test.kt:10 foo
// test.kt:11 foo
// test.kt:15 box
// test.kt:5 foo
// test.kt:6 foo
// test.kt:7 foo
// test.kt:8 foo
// test.kt:10 foo
// test.kt:11 foo
// test.kt:16 box

// EXPECTATIONS JS_IR
// test.kt:14 box
// test.kt:6 foo
// test.kt:11 foo
// test.kt:15 box
// test.kt:6 foo
// test.kt:6 foo
// test.kt:7 foo
// test.kt:7 foo
// test.kt:11 foo
// test.kt:16 box

// EXPECTATIONS WASM
// test.kt:14 $box (8, 4)
// test.kt:6 $foo (12, 12, 31, 31, 25)
// test.kt:10 $foo (4, 4, 4, 4, 4, 4, 4, 4, 4, 4)
// test.kt:11 $foo (1, 1)
// test.kt:15 $box (8, 4)
// test.kt:5 $foo (4, 4)
// test.kt:7 $foo (27, 27, 13)
// test.kt:8 $foo (8, 8, 8)
// test.kt:16 $box
