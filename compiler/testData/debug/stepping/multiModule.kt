
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

// EXPECTATIONS WASM
// test.kt:15 $box (4, 4)
// a.kt:5 $a (10, 10, 10, 10, 13)
// test.kt:16 $box
// b.kt:9 $b (10, 10, 10, 10, 13)
// test.kt:17 $box
