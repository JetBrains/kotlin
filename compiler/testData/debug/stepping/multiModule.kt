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

// EXPECTATIONS JVM_IR
// test.kt:14 box
// a.kt:4 a
// test.kt:14 box
// test.kt:15 box
// b.kt:8 b
// test.kt:15 box
// test.kt:16 box

// EXPECTATIONS NATIVE
// test.kt:14 box
// a.kt:4 a
// test.kt:14 box
// test.kt:15 box
// b.kt:8 b
// test.kt:16 box

// EXPECTATIONS JS_IR
// test.kt:14 box
// a.kt:4 a
// test.kt:15 box
// b.kt:8 b
// test.kt:16 box

// EXPECTATIONS WASM
// test.kt:14 $box (4)
// a.kt:4 $a (10, 13)
// test.kt:14 $box (4)
// test.kt:15 $box (4)
// b.kt:8 $b (10, 13)
// test.kt:15 $box (4)
// test.kt:16 $box (1)
