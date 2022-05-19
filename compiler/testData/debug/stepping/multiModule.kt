// MODULE: lib
// FILE: a.kt

fun a() = "a"

// FILE: b.kt

fun b() = "b"

// MODULE: main(lib)
// FILE: test.kt

fun box() {
    a()
    b()
}

// EXPECTATIONS
// test.kt:14 box
// a.kt:4 a
// test.kt:14 box
// test.kt:15 box
// b.kt:8 b
// test.kt:15 box
// test.kt:16 box
