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
// EXPECTATIONS JVM JVM_IR
// test.kt:4 box
// test.kt:8 foo$default (synthetic)
// test.kt:9 foo
// test.kt:8 foo$default (synthetic)
// test.kt:5 box
// test.kt:11 box
// test.kt:12 box
// test.kt:6 box

// EXPECTATIONS JS_IR
// test.kt:4 box
// test.kt:9 foo
// test.kt:6 box
