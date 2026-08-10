// WASM_IGNORE_FOR: mode=single-module
// WASM_IGNORE_FOR: mode=multi-module
// ^^^ Both of these: KT-88234
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
// test.kt:17 box
// a.kt:7 a
// test.kt:17 box
// test.kt:18 box
// b.kt:11 b
// test.kt:18 box
// test.kt:19 box

// EXPECTATIONS NATIVE
// test.kt:17 box
// a.kt:7 a
// test.kt:17 box
// test.kt:18 box
// b.kt:11 b
// test.kt:19 box

// EXPECTATIONS JS_IR
// test.kt:17 box
// a.kt:7 a
// test.kt:18 box
// b.kt:11 b
// test.kt:19 box

// EXPECTATIONS WASM
// test.kt:17 $box (4)
// a.kt:7 $a (10, 13)
// test.kt:17 $box (4)
// test.kt:18 $box (4)
// b.kt:11 $b (10, 13)
// test.kt:18 $box (4)
// test.kt:19 $box (1)
