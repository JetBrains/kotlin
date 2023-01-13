

// FILE: test.kt
fun foo() {}

fun box() {
    foo()
}

// EXPECTATIONS
// test.kt:7 box:
// test.kt:4 foo:
// test.kt:8 box:
