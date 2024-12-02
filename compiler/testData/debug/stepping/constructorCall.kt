
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

// EXPECTATIONS JVM_IR
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

// EXPECTATIONS WASM
// test.kt:6 $box (10)
// test.kt:7 $box (6, 4)
// test.kt:4 $A.<init> (7, 8, 19)
// test.kt:9 $box (8)
// test.kt:8 $box (4)
// test.kt:11 $box (6, 4)
// test.kt:14 $box (8)
// test.kt:13 $box (4)
// test.kt:15 $box (1)
