// IGNORE_BACKEND_K1: ANY
// DUMP_IR
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
// test.kt:8 box:
// test.kt:4 writeFalse:
// test.kt:8 box:
// test.kt:9 box: _:boolean=false:boolean
// test.kt:5 writeTrue:
// test.kt:9 box: _:boolean=false:boolean
// test.kt:11 box: _:boolean=true:boolean
// test.kt:4 writeFalse:
// test.kt:11 box: _:boolean=true:boolean
// test.kt:12 box: _:boolean=false:boolean
// test.kt:13 box: _:boolean=false:boolean
// test.kt:16 box: _:boolean=true:boolean
// test.kt:17 box: _:boolean=true:boolean
