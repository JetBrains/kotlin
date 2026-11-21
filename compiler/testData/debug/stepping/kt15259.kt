

// FILE: test.kt
interface ObjectFace

private fun makeFace() = object : ObjectFace {

    init { 5 }
}

fun box() {
    makeFace()
}

// EXPECTATIONS JVM
// test.kt:12 box
// test.kt:6 makeFace
// test.kt:6 <init>
// test.kt:8 <init>
// test.kt:6 <init>
// test.kt:9 makeFace
// test.kt:12 box
// test.kt:13 box

// EXPECTATIONS NATIVE
// test.kt:12 box
// test.kt:6 makeFace
// test.kt:6 <init>
// test.kt:9 <init>
// test.kt:6 makeFace
// test.kt:9 makeFace
// test.kt:13 box

// EXPECTATIONS JS_IR JS_IR_ES6
// test.kt:12 box
// test.kt:9 makeFace
// EXPECTATIONS JS_IR
// test.kt:6 <init>
// EXPECTATIONS JS_IR JS_IR_ES6
// test.kt:13 box

// EXPECTATIONS WASM_JS
// test.kt:12 $box (4)
// test.kt:6 $makeFace (25)
// test.kt:8 $<no name provided>.<init> (11)
// test.kt:9 $<no name provided>.<init> (1)
// test.kt:9 $makeFace (1)
// test.kt:12 $box (4)
// test.kt:13 $box (1)
