
// WITH_STDLIB
// FILE: test.kt
suspend fun foo(block: Long.() -> String): String {
    return 1L.block()
}

suspend fun box() {
    foo {
        "OK"
    }
}

// EXPECTATIONS JVM_IR
// test.kt:9 box
// test.kt:5 foo
// EXPECTATIONS ClassicFrontend JVM_IR
// test.kt:10 invoke
// EXPECTATIONS FIR JVM_IR
// test.kt:10 box$lambda$0
// EXPECTATIONS JVM_IR
// test.kt:5 foo
// test.kt:9 box
// test.kt:12 box

// EXPECTATIONS JS_IR
// test.kt:9 doResume
// test.kt:5 foo
// test.kt:5 foo
// test.kt:10 box$lambda
// test.kt:12 doResume

// EXPECTATIONS WASM
