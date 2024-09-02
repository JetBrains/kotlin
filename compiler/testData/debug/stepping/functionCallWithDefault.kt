
// FILE: test.kt

fun box() {
    foo()
    bar()
}

fun foo(i: Int = 1) {
}

inline fun bar(i: Int = 1) {
}

// FORCE_STEP_INTO
// EXPECTATIONS JVM_IR
// test.kt:5 box
// test.kt:9 foo$default (synthetic)
// test.kt:10 foo
// test.kt:9 foo$default (synthetic)
// test.kt:6 box
// test.kt:12 box
// test.kt:13 box
// test.kt:7 box

// EXPECTATIONS JS_IR
// test.kt:5 box
// test.kt:10 foo
// test.kt:7 box

// EXPECTATIONS WASM
// test.kt:5 $box (4, 4)
// test.kt:9 $foo$default (0, 0, 0, 17, 17, 0)
// test.kt:10 $foo
// test.kt:6 $box
// test.kt:12 $box (24, 24)
// test.kt:7 $box
