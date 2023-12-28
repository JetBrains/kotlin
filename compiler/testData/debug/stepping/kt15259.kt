

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
// test.kt:1 $box
// test.kt:12 $box (4, 4)
// test.kt:6 $makeFace (25, 25)
// test.kt:9 $<no name provided>.<init>
// test.kt:9 $makeFace
// test.kt:13 $box
