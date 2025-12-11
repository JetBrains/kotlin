
// FILE: test.kt

private fun foo() = "OK"

internal inline val a: () -> String
    get() = {
        foo()
    }

fun box(): String {
    return a.invoke()
}

// EXPECTATIONS JVM_IR
// test.kt:12 box
// test.kt:7 box
// test.kt:9 box
// test.kt:12 box
// test.kt:8 invoke
// test.kt:4 foo
// test.kt:8 invoke
// test.kt:12 box

// EXPECTATIONS NATIVE
// test.kt:12 box
// test.kt:7 box
// test.kt:12 box
// test.kt:7 invoke
// test.kt:8 invoke
// test.kt:1 access$foo$tTestKt
// test.kt:4 foo
// test.kt:1 access$foo$tTestKt
// test.kt:8 invoke
// test.kt:9 invoke
// test.kt:12 box
// test.kt:13 box

// EXPECTATIONS JS_IR
// test.kt:12 box
// test.kt:8 box$lambda
// test.kt:1 access$foo$tTestKt
// test.kt:4 foo

// EXPECTATIONS WASM
// test.kt:12 $box (11)
// test.kt:9 $box (5)
// test.kt:12 $box (13)
// test.kt:8 $box$lambda.invoke (8)
// test.kt:4 $foo (20, 24)
// test.kt:8 $box$lambda.invoke (13)
// test.kt:12 $box (13, 4)
