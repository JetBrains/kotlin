// IGNORE_BACKEND: JS_IR, WASM_JS
// ^^^ Those backends don't support exitProcess()
// WITH_STDLIB

// FILE: test.kt
fun box() {
    println("start")
    kotlin.system.exitProcess(0)
    println("unreachable")
}

// EXPECTATIONS JVM_IR
// test.kt:7 box
// test.kt:8 box

// EXPECTATIONS NATIVE
// test.kt:7 box
// test.kt:7 box
// test.kt:8 box
