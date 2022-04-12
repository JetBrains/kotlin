// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR
// FILE: test.kt
interface ObjectFace

private fun makeFace() = object : ObjectFace {

    init { 5 }
}

fun box() {
    makeFace()
}

// IR backend has additional steps on the way _out_ of the init block.

// EXPECTATIONS
// test.kt:12 box
// test.kt:6 makeFace
// test.kt:6 <init>
// test.kt:8 <init>
// test.kt:9 makeFace
// test.kt:12 box
// test.kt:13 box