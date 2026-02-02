// IGNORE_BACKEND_K1: ANY
// DUMP_IR
// LANGUAGE: +UnnamedLocalVariables
// FILE: test.kt
fun writeFalse(): Boolean = false
fun writeTrue(): Boolean = true

fun box() {
    val _ = writeFalse()
    val _ = writeTrue()

    when(val _ = writeFalse()) {
        true -> {}
        false -> {}
    }

    for (_ in 1..10) {}
}

// EXPECTATIONS FIR JVM_IR
// test.kt:9 box:
// test.kt:5 writeFalse:
// test.kt:9 box:
// test.kt:10 box:
// test.kt:6 writeTrue:
// test.kt:10 box:
// test.kt:12 box:
// test.kt:5 writeFalse:
// test.kt:12 box:
// test.kt:13 box:
// test.kt:14 box:
// test.kt:17 box:
// test.kt:18 box:

// EXPECTATIONS WASM
// test.kt:9 $box: (12)
// test.kt:5 $writeFalse: (28, 33)
// test.kt:9 $box: (12)
// test.kt:10 $box: (12)
// test.kt:6 $writeTrue: (27, 31)
// test.kt:10 $box: (12)
// test.kt:12 $box: (17)
// test.kt:5 $writeFalse: (28, 33)
// test.kt:13 $box: (8)
// test.kt:14 $box: (8)
// test.kt:17 $box: (14, 4, 17, 4, 14, 4, 17, 4, 14, 4, 17, 4, 14, 4, 17, 4, 14, 4, 17, 4, 14, 4, 17, 4, 14, 4, 17, 4, 14, 4, 17, 4, 14, 4, 17, 4, 14, 4, 17, 4, 14, 4, 17, 4)
// test.kt:18 $box: (1)
