
// FILE: test.kt

fun <T> eval(f: () -> T) = f()

fun box() {
    eval {
        "OK"
    }
}

// EXPECTATIONS JVM_IR
// test.kt:7 box
// test.kt:4 eval
// EXPECTATIONS ClassicFrontend JVM_IR
// test.kt:8 invoke
// EXPECTATIONS FIR JVM_IR
// test.kt:8 box$lambda$0
// EXPECTATIONS JVM_IR
// test.kt:4 eval
// test.kt:7 box
// test.kt:10 box

// EXPECTATIONS JS_IR
// test.kt:7 box
// test.kt:4 eval
// test.kt:8 box$lambda
// test.kt:10 box

// EXPECTATIONS WASM
// test.kt:7 $box
// test.kt:4 $eval (27, 27, 27, 30)
// test.kt:8 $box$lambda.invoke (8, 8, 8, 8, 12)
// test.kt:10 $box
