
// WITH_STDLIB
// TODO: Enable the test on JS BE, but now it is too flaky there.
// TARGET_BACKEND: JVM
// FILE: test.kt

import kotlin.coroutines.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        it.getOrThrow()
    })
}

suspend fun empty() {}

fun box() {
    builder {
        empty()
        builder {
            empty()
        }
    }
}

// EXPECTATIONS JVM_IR
// test.kt:18 box
// test.kt:10 builder
// Continuation.kt:66 <init>
// test.kt:10 builder
// Continuation.kt:68 getContext
// test.kt:-1 <init>
// test.kt:-1 create
// test.kt:15 empty
// test.kt:19 invokeSuspend
// test.kt:20 invokeSuspend
// test.kt:10 builder
// Continuation.kt:66 <init>
// test.kt:10 builder
// Continuation.kt:68 getContext
// test.kt:-1 <init>
// test.kt:-1 create
// test.kt:15 empty
// test.kt:21 invokeSuspend
// test.kt:22 invokeSuspend
// Continuation.kt:71 resumeWith
// test.kt:11 resumeWith
// test.kt:12 resumeWith
// Continuation.kt:71 resumeWith
// test.kt:13 builder
// test.kt:23 invokeSuspend
// Continuation.kt:71 resumeWith
// test.kt:11 resumeWith
// test.kt:12 resumeWith
// Continuation.kt:71 resumeWith
// test.kt:13 builder
// test.kt:24 box

// EXPECTATIONS WASM
