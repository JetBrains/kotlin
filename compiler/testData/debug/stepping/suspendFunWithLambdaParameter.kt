
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
// test.kt:12 $box (1)
// coroutineHelpers.kt:9 $EmptyContinuation.<get-context> (37)
// test.kt:12 $box (1)
// test.kt:9 $$boxCOROUTINE$.doResume (4)
// test.kt:5 $foo (14, 11, 14)
// test.kt:10 $box$lambda.invoke (8, 12)
// test.kt:5 $foo (14, 4)
// test.kt:9 $$boxCOROUTINE$.doResume (4)
// test.kt:12 $$boxCOROUTINE$.doResume (1)
// test.kt:9 $$boxCOROUTINE$.doResume (4)
// test.kt:12 $$boxCOROUTINE$.doResume (0)
// test.kt:12 $box (1)
