// IGNORE_BACKEND: WASM
// FILE: test.kt

// Comment before
fun foo(i: Int = 1): Int {
    return i
}

fun box() {
    foo()
}

// EXPECTATIONS JVM JVM_IR
// test.kt:10 box
// test.kt:6 foo
// test.kt:10 box
// test.kt:11 box

// EXPECTATIONS JS_IR
// test.kt:10 box
// test.kt:11 box
