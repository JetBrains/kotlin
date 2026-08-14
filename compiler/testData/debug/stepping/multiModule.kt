// WASM_IGNORE_FOR: mode=single-module
// ^^^ KT-88234
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
// test.kt:16 box
// a.kt:6 a
// test.kt:16 box
// test.kt:17 box
// b.kt:10 b
// test.kt:17 box
// test.kt:18 box

// EXPECTATIONS NATIVE
// test.kt:16 box
// a.kt:6 a
// test.kt:16 box
// test.kt:17 box
// b.kt:10 b
// test.kt:18 box

// EXPECTATIONS JS_IR
// test.kt:16 box
// a.kt:6 a
// test.kt:17 box
// b.kt:10 b
// test.kt:18 box

// EXPECTATIONS WASM
// test.kt:16 $box (4)
// a.kt:6 $a (10, 13)
// test.kt:16 $box (4)
// test.kt:17 $box (4)
// b.kt:10 $b (10, 13)
// test.kt:17 $box (4)
// test.kt:18 $box (1)
