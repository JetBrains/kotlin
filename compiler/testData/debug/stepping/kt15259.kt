// IGNORE_BACKEND: JVM_IR
// FILE: test.kt
interface ObjectFace

private fun makeFace() = object : ObjectFace {

    init { 5 }
}

fun box() {
    makeFace()
}

// IR backend has additional steps on the way _out_ of the init block.

// EXPECTATIONS JVM JVM_IR
// test.kt:11 box
// test.kt:5 makeFace
// test.kt:5 <init>
// test.kt:7 <init>
// test.kt:8 makeFace
// test.kt:11 box
// test.kt:12 box

// EXPECTATIONS JS_IR
// test.kt:11 box
// test.kt:8 makeFace
// test.kt:8 makeFace