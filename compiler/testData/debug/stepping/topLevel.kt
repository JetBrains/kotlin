// FILE: test.kt

fun foo() = prop

val prop = 1

fun box() {
    foo()
}

// LINENUMBERS
// test.kt:8 box
// test.kt:3 foo
// test.kt:8 box
// test.kt:9 box
