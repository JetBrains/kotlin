
// FILE: test.kt

fun box() {
    foo(
            1 + 1
    )
}

fun foo(i: Int) {
}

// EXPECTATIONS JVM_IR
// test.kt:6 box
// test.kt:5 box
// test.kt:11 foo
// test.kt:8 box

// EXPECTATIONS JS_IR
// test.kt:5 box
// test.kt:11 foo
// test.kt:8 box

// EXPECTATIONS WASM
// test.kt:1 $box
// test.kt:6 $box
// test.kt:5 $box
// test.kt:11 $foo
// test.kt:8 $box
