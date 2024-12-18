

// FILE: test.kt
interface ObjectFace

private fun makeFace() = object : ObjectFace {

    init { 5 }
}

fun box() {
    makeFace()
}

// EXPECTATIONS JVM_IR
// test.kt:12 box
// test.kt:6 makeFace
// test.kt:6 <init>
// test.kt:8 <init>
// test.kt:6 <init>
// test.kt:9 makeFace
// test.kt:12 box
// test.kt:13 box

// EXPECTATIONS JS_IR
// test.kt:12 box
// test.kt:9 makeFace
// test.kt:6 <init>
// test.kt:13 box

// EXPECTATIONS WASM
// test.kt:12 $box (4)
// test.kt:6 $makeFace (25)
// test.kt:8 $<no name provided>.<init> (11)
// test.kt:9 $<no name provided>.<init> (1)
// test.kt:9 $makeFace (1)
// test.kt:13 $box (1)
