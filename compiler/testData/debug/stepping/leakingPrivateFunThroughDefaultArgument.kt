
// FILE: test.kt

private fun foo() = "OK"

internal inline fun bar(
    ok: String = foo()
) =
    ok

fun box(): String {
    return bar()
}

// EXPECTATIONS JVM_IR
// test.kt:12 box
// test.kt:7 box
// test.kt:4 foo
// test.kt:7 box
// test.kt:6 box
// test.kt:9 box
// test.kt:12 box

// EXPECTATIONS NATIVE
// test.kt:12 box
// test.kt:7 box
// test.kt:1 access$foo$tTestKt
// test.kt:4 foo
// test.kt:1 access$foo$tTestKt
// test.kt:7 box
// test.kt:9 box
// test.kt:12 box
// test.kt:13 box

// EXPECTATIONS JS_IR
// test.kt:12 box
// test.kt:1 access$foo$tTestKt
// test.kt:4 foo

// EXPECTATIONS WASM
// test.kt:12 $box (11)
// test.kt:7 $box (17)
// test.kt:4 $foo (20, 24)
// test.kt:9 $box (4, 6)
// test.kt:12 $box (4)
