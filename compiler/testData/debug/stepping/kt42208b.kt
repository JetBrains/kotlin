// IGNORE_BACKEND_K2_MULTI_MODULE: JS_IR
// ^^^ Cross-module inliner wrongly uses filename for call site instead of correct filename for inline fun declaration
// FILE: test.kt

fun box() {
    val a = foo()
    a()
}
// FILE: test1.kt
// aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
inline fun foo() = {
}

// EXPECTATIONS JVM_IR
// test.kt:6 box
// test1.kt:11 box
// test1.kt:12 box
// test.kt:6 box
// test.kt:7 box
// test1.kt:12 invoke
// test.kt:7 box
// test.kt:8 box

// EXPECTATIONS JS_IR
// test1.kt:11 box
// test.kt:7 box
// test.kt:2 box$lambda
// test.kt:8 box

// EXPECTATIONS WASM
// test.kt:6 $box (12)
// test1.kt:12 $box (1)
// test.kt:7 $box (4)

// EXPECTATIONS ClassicFrontend WASM
// test1.kt:12 $box$lambda.invoke (0)

// EXPECTATIONS FIR WASM
// test1.kt:12 $box$lambda.invoke (1)

// EXPECTATIONS WASM
// test.kt:7 $box (4)
// test.kt:8 $box (1)
