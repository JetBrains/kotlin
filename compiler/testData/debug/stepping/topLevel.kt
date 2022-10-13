// FILE: test.kt

fun foo() = prop

val prop = 1

fun box() {
    foo()
}

// EXPECTATIONS JVM JVM_IR
// test.kt:8 box
// test.kt:3 foo
// test.kt:8 box
// test.kt:9 box

// EXPECTATIONS JS_IR
// test.kt:8 box
// test.kt:3 foo
// test.kt:9 box