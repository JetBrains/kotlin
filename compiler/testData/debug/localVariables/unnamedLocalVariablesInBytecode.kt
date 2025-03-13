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
// test.kt:12 box:
// test.kt:5 writeFalse:
// test.kt:12 box:
// test.kt:13 box:
// test.kt:14 box:
// test.kt:17 box:
// test.kt:18 box:
