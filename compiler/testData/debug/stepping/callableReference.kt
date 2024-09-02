
// FILE: test.kt
fun box() {
    var x = false
    f {
        x = true
    }
}

fun f(block: () -> Unit) {
    block()
}

// EXPECTATIONS JVM_IR
// test.kt:4 box
// test.kt:5 box
// test.kt:11 f
// EXPECTATIONS ClassicFrontend JVM_IR
// test.kt:6 invoke
// test.kt:7 invoke
// EXPECTATIONS FIR JVM_IR
// test.kt:6 box$lambda$0
// test.kt:7 box$lambda$0
// EXPECTATIONS JVM_IR
// test.kt:11 f
// test.kt:12 f
// test.kt:8 box

// EXPECTATIONS JS_IR
// test.kt:4 box
// test.kt:5 box
// test.kt:5 box$lambda
// test.kt:5 box
// test.kt:11 f
// test.kt:6 box$lambda$lambda
// test.kt:7 box$lambda$lambda
// test.kt:12 f
// test.kt:8 box

// EXPECTATIONS WASM
// test.kt:4 $box (12, 12, 4)
// test.kt:5 $box (6, 6, 4)
// test.kt:11 $f (4, 4, 4)
// test.kt:6 $box$lambda.invoke (8, 12, 8, 16)
// test.kt:12 $f
// test.kt:8 $box
