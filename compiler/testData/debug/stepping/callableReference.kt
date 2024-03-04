
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
// Runtime.kt:70 $kotlin.wasm.internal.getBoxedBoolean (8, 8)
// Runtime.kt:73 $kotlin.wasm.internal.getBoxedBoolean (8, 35)
// Standard.kt:71 $kotlin.wasm.internal.getBoxedBoolean (0, 0, 0, 0)
// Standard.kt:95 $kotlin.wasm.internal.getBoxedBoolean (4, 4)
// Standard.kt:98 $kotlin.wasm.internal.getBoxedBoolean (4, 10, 4, 10)
// Standard.kt:74 $kotlin.wasm.internal.getBoxedBoolean (15, 7)
// Standard.kt:99 $kotlin.wasm.internal.getBoxedBoolean (11, 4, 11, 4)
// Runtime.kt:74 $kotlin.wasm.internal.getBoxedBoolean (5, 5)
// test.kt:5 $box (6, 6, 4)
// test.kt:11 $f
// test.kt:6 $box$lambda.invoke (8, 12, 8, 16)
// Runtime.kt:71 $kotlin.wasm.internal.getBoxedBoolean (8, 33)
// Standard.kt:68 $kotlin.wasm.internal.getBoxedBoolean (25, 25, 25, 25, 45, 38)
// test.kt:12 $f
// test.kt:8 $box
