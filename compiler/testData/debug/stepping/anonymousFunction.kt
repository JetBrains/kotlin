// IGNORE_BACKEND: WASM
// FILE: test.kt

fun <T> eval(f: () -> T) = f()

fun box() {
    eval {
        "OK"
    }
}

// EXPECTATIONS JVM JVM_IR
// test.kt:7 box
// test.kt:4 eval
// test.kt:8 invoke
// test.kt:4 eval
// test.kt:7 box
// test.kt:10 box

// EXPECTATIONS JS_IR
// test.kt:7 box
// test.kt:4 eval
// test.kt:8 box$lambda
// test.kt:10 box