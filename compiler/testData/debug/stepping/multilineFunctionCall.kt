
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
// test.kt:4 $box (10)
// test.kt:6 $box (12)
// test.kt:5 $box (4)
// test.kt:10 $foo (16)
// test.kt:11 $foo (1)
// test.kt:8 $box (1)
