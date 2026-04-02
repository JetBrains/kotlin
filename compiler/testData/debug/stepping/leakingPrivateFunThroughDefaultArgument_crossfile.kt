
// FILE: test.kt

private fun foo() = "OK"

internal inline fun bar(
    ok: String = foo()
) =
    ok

// FILE: box.kt

fun box(): String {
    return bar()
}

// EXPECTATIONS JVM_IR

// EXPECTATIONS NATIVE
// box.kt:14 box
// test.kt:7 box
// test.kt:1 access$foo$tTestKt
// test.kt:4 foo
// test.kt:1 access$foo$tTestKt
// test.kt:7 box
// test.kt:9 box
// box.kt:14 box
// box.kt:15 box

// EXPECTATIONS JS_IR
// box.kt:14 box
// test.kt:1 access$foo$tTestKt
// test.kt:4 foo

// EXPECTATIONS WASM
// box.kt:14 $box (11)
// test.kt:7 $box (17)
// test.kt:4 $foo (20, 24)
// test.kt:9 $box (4, 6)
// box.kt:14 $box (4)
