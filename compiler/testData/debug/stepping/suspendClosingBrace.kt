
// WITH_STDLIB
// FILE: test.kt

suspend fun yield() {}

suspend fun foo(flag: Boolean) {
    if (flag) {
        yield()
    }
    println()
}

suspend fun box() {
    foo(false)

    foo(true)
}

// EXPECTATIONS JVM_IR
// test.kt:14 box
// test.kt:15 box
// test.kt:7 foo
// test.kt:8 foo
// test.kt:11 foo
// test.kt:12 foo
// test.kt:15 box
// test.kt:17 box
// test.kt:7 foo
// test.kt:8 foo
// test.kt:9 foo
// test.kt:5 yield
// test.kt:9 foo
// test.kt:11 foo
// test.kt:12 foo
// test.kt:17 box
// test.kt:18 box

// EXPECTATIONS JS_IR
// test.kt:15 doResume
// test.kt:8 doResume
// test.kt:11 doResume
// test.kt:12 doResume
// test.kt:17 doResume
// test.kt:8 doResume
// test.kt:9 doResume
// test.kt:5 yield
// test.kt:11 doResume
// test.kt:12 doResume
// test.kt:18 doResume

// EXPECTATIONS: WASM
// coroutineHelpers.kt:9 $EmptyContinuation.<get-context> (37)
// test.kt:15 $$boxCOROUTINE$1.doResume (8, 4)
// test.kt:8 $$fooCOROUTINE$0.doResume (8)
// test.kt:11 $$fooCOROUTINE$0.doResume (4)
// test.kt:12 $$fooCOROUTINE$0.doResume (0)
// test.kt:17 $$boxCOROUTINE$1.doResume (8, 4)
// test.kt:8 $$fooCOROUTINE$0.doResume (8)
// test.kt:9 $$fooCOROUTINE$0.doResume (8)
// test.kt:5 $yield (21)
// test.kt:11 $$fooCOROUTINE$0.doResume (4)
// test.kt:12 $$fooCOROUTINE$0.doResume (0)
// test.kt:18 $$boxCOROUTINE$1.doResume (0)
