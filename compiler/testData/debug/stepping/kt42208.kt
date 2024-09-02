
// IGNORE_INLINER: IR
// FILE: test.kt

fun box() {
    foo()()
}
// FILE: test1.kt
// aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
inline fun foo() = {
}

// EXPECTATIONS JVM_IR
// test.kt:6 box
// test1.kt:10 box
// test1.kt:11 box
// test.kt:6 box
// test1.kt:11 invoke
// test.kt:6 box
// test.kt:7 box

// EXPECTATIONS JS_IR
// test1.kt:10 box
// test1.kt:8 box$lambda
// test.kt:7 box

// EXPECTATIONS WASM
// test.kt:6 $box (4, 4, 4, 4)
// test1.kt:11 $box
// test.kt:8 $box$lambda.invoke
// test.kt:7 $box
