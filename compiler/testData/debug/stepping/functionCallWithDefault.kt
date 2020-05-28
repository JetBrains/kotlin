// FILE: test.kt

fun box() {
    foo()
    bar()
}

fun foo(i: Int = 1) {
}

inline fun bar(i: Int = 1) {
}

// The JVM_IR backend has line number 11 for the inlined
// default argument handling both before and after the actual
// body of bar. That is consistent with what happens with the
// $default method in the non-inlined case.

// FORCE_STEP_INTO
// LINENUMBERS
// test.kt:4 box
// test.kt:8 foo$default (synthetic)
// test.kt:9 foo
// test.kt:8 foo$default (synthetic)
// test.kt:5 box
// test.kt:11 box
// test.kt:12 box
// LINENUMBERS JVM_IR
// test.kt:11 box
// LINENUMBERS
// test.kt:6 box
