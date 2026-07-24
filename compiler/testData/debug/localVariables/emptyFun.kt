

// FILE: test.kt
fun foo() {}

fun box() {
    foo()
}

// EXPECTATIONS JVM_IR
// test.kt:7 box:
// test.kt:4 foo:
// test.kt:8 box:

// EXPECTATIONS WASM
// test.kt:7 $box: (4)
// test.kt:4 $foo: (12)
// test.kt:8 $box: (1)
