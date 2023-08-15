// IGNORE_BACKEND: WASM
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

// EXPECTATIONS JVM JVM_IR
// test.kt:15 box
// a.kt:5 a
// test.kt:15 box
// test.kt:16 box
// b.kt:9 b
// test.kt:16 box
// test.kt:17 box

// EXPECTATIONS JS_IR
// test.kt:15 box
// a.kt:5 a
// test.kt:16 box
// b.kt:9 b
// test.kt:17 box