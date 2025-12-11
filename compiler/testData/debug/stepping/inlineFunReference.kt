
// FILE: test.kt

inline fun foo() = "OK"

fun bar() =
    ::foo

fun box(): String {
    return bar().invoke()
}

// EXPECTATIONS JVM_IR
// test.kt:10 box
// test.kt:7 bar
// test.kt:10 box
// test.kt:7 invoke
// test.kt:4 invoke
// test.kt:7 invoke
// test.kt:10 box

// EXPECTATIONS NATIVE
// test.kt:10 box
// test.kt:6 bar
// test.kt:7 bar
// test.kt:10 box
// test.kt:7 invoke
// test.kt:4 invoke
// test.kt:7 invoke
// test.kt:10 box
// test.kt:11 box

// EXPECTATIONS JS_IR
// test.kt:10 box
// test.kt:7 bar
// test.kt:7 foo$ref
// test.kt:10 box
// test.kt:4 foo$ref$lambda

// EXPECTATIONS WASM
// test.kt:10 $box (11)
// test.kt:7 $bar (9)
// test.kt:10 $box (17)
// test.kt:7 $foo$ref.invoke (4)
// test.kt:4 $foo$ref.invoke (19, 23)
// test.kt:10 $box (17, 4)
