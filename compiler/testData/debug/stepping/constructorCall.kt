// IGNORE_BACKEND: WASM
// FILE: test.kt

class A(val x: Int)

fun box() {
    A(1)
    A(
        2
    )
    A(3
    )
    A(
        4)
}

// EXPECTATIONS JVM JVM_IR
// test.kt:7 box
// test.kt:4 <init>
// test.kt:7 box
// test.kt:8 box
// test.kt:9 box
// test.kt:8 box
// test.kt:4 <init>
// test.kt:8 box
// test.kt:11 box
// test.kt:4 <init>
// test.kt:11 box
// test.kt:13 box
// test.kt:14 box
// test.kt:13 box
// test.kt:4 <init>
// test.kt:13 box
// test.kt:15 box

// EXPECTATIONS JS_IR
// test.kt:7 box
// test.kt:4 <init>
// test.kt:4 <init>
// test.kt:8 box
// test.kt:4 <init>
// test.kt:4 <init>
// test.kt:11 box
// test.kt:4 <init>
// test.kt:4 <init>
// test.kt:13 box
// test.kt:4 <init>
// test.kt:4 <init>
// test.kt:15 box