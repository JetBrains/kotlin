
// FILE: test.kt

private fun foo() = "OK"

internal inline val a: () -> String
    get() = {
        foo()
    }

// FILE: box.kt

fun box(): String {
    return a.invoke()
}

// EXPECTATIONS JVM_IR

// EXPECTATIONS NATIVE
// box.kt:14 box
// test.kt:7 box
// box.kt:14 box
// test.kt:7 invoke
// test.kt:8 invoke
// test.kt:1 access$foo$tTestKt
// test.kt:4 foo
// test.kt:1 access$foo$tTestKt
// test.kt:8 invoke
// test.kt:9 invoke
// box.kt:14 box
// box.kt:15 box

// EXPECTATIONS JS_IR
// box.kt:14 box
// box.kt:19 box$lambda
// test.kt:1 access$foo$tTestKt
// test.kt:4 foo

// EXPECTATIONS WASM
// box.kt:14 $box (11)
// test.kt:9 $box (5)
// box.kt:14 $box (13)
// test.kt:8 $box$lambda.invoke (8)
// test.kt:4 $foo (20, 24)
// test.kt:8 $box$lambda.invoke (13)
// box.kt:14 $box (13, 4)
