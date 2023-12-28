
// FILE: test.kt

fun foo() = prop

val prop = 1

fun box() {
    foo()
}

// EXPECTATIONS JVM_IR
// test.kt:9 box
// test.kt:4 foo
// test.kt:9 box
// test.kt:10 box

// EXPECTATIONS JS_IR
// test.kt:9 box
// test.kt:4 foo
// test.kt:10 box

// EXPECTATIONS WASM
// test.kt:1 $box
// test.kt:9 $box (4, 4)
// test.kt:4 $foo (12, 16)
// test.kt:10 $box
