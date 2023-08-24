// IGNORE_BACKEND: WASM
// IGNORE_INLINER: IR
// FILE: test.kt

fun box() {
    val a = foo()
    a()
}
// FILE: test1.kt
// aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
inline fun foo() = {
}
// EXPECTATIONS JVM JVM_IR
// test.kt:6 box
// test1.kt:11 box
// test1.kt:12 box
// EXPECTATIONS JVM_IR
// test.kt:6 box
// test.kt:7 box
// test1.kt:12 invoke
// test.kt:7 box
// EXPECTATIONS JVM_IR
// test.kt:8 box
// EXPECTATIONS JVM
// test.kt:6 box
// test.kt:7 box
// test.kt:8 box

// EXPECTATIONS JS_IR
// test1.kt:11 box
// test.kt:7 box
// test1.kt:9 box$lambda
// test.kt:8 box
