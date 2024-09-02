
// FILE: test.kt

// Comment before
fun foo(i: Int = 1): Int {
    return i
}

fun box() {
    foo()
}

// EXPECTATIONS JVM_IR
// test.kt:10 box
// test.kt:6 foo
// test.kt:10 box
// test.kt:11 box

// EXPECTATIONS JS_IR
// test.kt:10 box
// test.kt:11 box

// EXPECTATIONS WASM
// test.kt:10 $box (4, 4, 4, 4)
// test.kt:5 $foo$default (0, 0, 0, 17, 17, 0)
// test.kt:6 $foo (11, 4)
// test.kt:11 $box
