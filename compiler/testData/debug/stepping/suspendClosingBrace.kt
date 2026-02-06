// ENHANCED_COROUTINES_DEBUGGING
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
// GeneratedCodeMarkers.kt:... box
// GeneratedCodeMarkers.kt:... box
// test.kt:14 box
// GeneratedCodeMarkers.kt:... box
// test.kt:15 box
// GeneratedCodeMarkers.kt:... foo
// GeneratedCodeMarkers.kt:... foo
// test.kt:7 foo
// GeneratedCodeMarkers.kt:... foo
// test.kt:8 foo
// test.kt:11 foo
// test.kt:12 foo
// GeneratedCodeMarkers.kt:... box
// GeneratedCodeMarkers.kt:... box
// test.kt:17 box
// GeneratedCodeMarkers.kt:... foo
// GeneratedCodeMarkers.kt:... foo
// test.kt:7 foo
// GeneratedCodeMarkers.kt:... foo
// test.kt:8 foo
// test.kt:9 foo
// test.kt:5 yield
// GeneratedCodeMarkers.kt:... foo
// GeneratedCodeMarkers.kt:... foo
// test.kt:11 foo
// test.kt:12 foo
// GeneratedCodeMarkers.kt:... box
// test.kt:18 box

// EXPECTATIONS NATIVE
// test.kt:14 box
// test.kt:14 invokeSuspend
// test.kt:14 invokeSuspend
// test.kt:15 invokeSuspend
// test.kt:7 foo
// test.kt:7 foo
// test.kt:7 invokeSuspend
// test.kt:7 invokeSuspend
// test.kt:11 invokeSuspend
// test.kt:7 invokeSuspend
// test.kt:12 invokeSuspend
// test.kt:7 foo
// test.kt:12 foo
// test.kt:15 invokeSuspend
// test.kt:15 invokeSuspend
// test.kt:17 invokeSuspend
// test.kt:7 foo
// test.kt:7 foo
// test.kt:7 invokeSuspend
// test.kt:7 invokeSuspend
// test.kt:9 invokeSuspend
// test.kt:5 yield
// test.kt:9 invokeSuspend
// test.kt:9 invokeSuspend
// test.kt:11 invokeSuspend
// test.kt:7 invokeSuspend
// test.kt:12 invokeSuspend
// test.kt:7 foo
// test.kt:12 foo
// test.kt:17 invokeSuspend
// test.kt:17 invokeSuspend
// test.kt:14 invokeSuspend
// test.kt:18 invokeSuspend
// test.kt:14 box
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
// test.kt:18 $box (1)
// coroutineHelpers.kt:9 $EmptyContinuation.<get-context> (37)
// test.kt:18 $box (1)
// test.kt:15 $$boxCOROUTINE$.doResume (8, 4)
// test.kt:12 $foo (1)
// test.kt:8 $$fooCOROUTINE$.doResume (8)
// test.kt:9 $$fooCOROUTINE$.doResume (8)
// test.kt:12 $$fooCOROUTINE$.doResume (1)
// test.kt:8 $$fooCOROUTINE$.doResume (8)
// test.kt:9 $$fooCOROUTINE$.doResume (8)
// test.kt:11 $$fooCOROUTINE$.doResume (4)
// test.kt:12 $$fooCOROUTINE$.doResume (0)
// test.kt:12 $foo (1)
// test.kt:15 $$boxCOROUTINE$.doResume (4)
// test.kt:18 $$boxCOROUTINE$.doResume (1)
// test.kt:15 $$boxCOROUTINE$.doResume (8, 4)
// test.kt:17 $$boxCOROUTINE$.doResume (8, 4)
// test.kt:12 $foo (1)
// test.kt:8 $$fooCOROUTINE$.doResume (8)
// test.kt:9 $$fooCOROUTINE$.doResume (8)
// test.kt:5 $yield (21)
// test.kt:9 $$fooCOROUTINE$.doResume (8)
// test.kt:12 $$fooCOROUTINE$.doResume (1)
// test.kt:8 $$fooCOROUTINE$.doResume (8)
// test.kt:9 $$fooCOROUTINE$.doResume (8)
// test.kt:12 $$fooCOROUTINE$.doResume (1)
// test.kt:8 $$fooCOROUTINE$.doResume (8)
// test.kt:9 $$fooCOROUTINE$.doResume (8)
// test.kt:11 $$fooCOROUTINE$.doResume (4)
// test.kt:12 $$fooCOROUTINE$.doResume (0)
// test.kt:12 $foo (1)
// test.kt:17 $$boxCOROUTINE$.doResume (4)
// test.kt:18 $$boxCOROUTINE$.doResume (1)
// test.kt:15 $$boxCOROUTINE$.doResume (8, 4)
// test.kt:17 $$boxCOROUTINE$.doResume (4)
// test.kt:18 $$boxCOROUTINE$.doResume (0)
// test.kt:18 $box (1)
