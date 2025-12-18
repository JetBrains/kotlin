
// FILE: test.kt

private fun foo() = "OK"

internal inline fun bar() =
    ::foo

fun box(): String {
    return bar().invoke()
}

// EXPECTATIONS JVM_IR
// test.kt:10 box
// test.kt:7 box
// test.kt:10 box
// test.kt:7 invoke
// test.kt:4 foo
// test.kt:7 invoke
// test.kt:10 box

// EXPECTATIONS NATIVE
// test.kt:10 box
// test.kt:7 box
// test.kt:10 box
// test.kt:1 access$foo$tTestKt
// test.kt:1 access$foo$tTestKt
// test.kt:10 box
// test.kt:11 box

// EXPECTATIONS JS_IR
// test.kt:10 box
// test.kt:7 foo$ref
// test.kt:10 box
// test.kt:1 access$foo$tTestKt
// test.kt:4 foo

// EXPECTATIONS WASM
// test.kt:10 $box (11)
// test.kt:7 $box (9)
// test.kt:10 $box (17)
// test.kt:7 $foo$ref.invoke (4)
// test.kt:4 $foo (20, 24)
// test.kt:7 $foo$ref.invoke (4)
// test.kt:10 $box (17, 4)
