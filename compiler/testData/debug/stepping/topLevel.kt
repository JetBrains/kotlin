// IGNORE_BACKEND: WASM
// FILE: test.kt

fun foo() = prop

val prop = 1

fun box() {
    foo()
}

// EXPECTATIONS JVM JVM_IR
// test.kt:9 box
// test.kt:4 foo
// test.kt:9 box
// test.kt:10 box

// EXPECTATIONS JS_IR
// test.kt:9 box
// test.kt:4 foo
// test.kt:10 box