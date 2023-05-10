// WITH_STDLIB
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

// EXPECTATIONS JVM JVM_IR
// test.kt:15 box
// test.kt:7 builder
// Continuation.kt:66 <init>
// test.kt:7 builder
// Continuation.kt:68 getContext
// test.kt:-1 <init>
// test.kt:-1 create
// test.kt:12 empty
// test.kt:16 invokeSuspend
// test.kt:17 invokeSuspend
// test.kt:7 builder
// Continuation.kt:66 <init>
// test.kt:7 builder
// Continuation.kt:68 getContext
// test.kt:-1 <init>
// test.kt:-1 create
// test.kt:12 empty
// test.kt:18 invokeSuspend
// test.kt:19 invokeSuspend
// Continuation.kt:71 resumeWith
// test.kt:8 resumeWith
// test.kt:9 resumeWith
// Continuation.kt:71 resumeWith
// test.kt:10 builder
// test.kt:20 invokeSuspend
// Continuation.kt:71 resumeWith
// test.kt:8 resumeWith
// test.kt:9 resumeWith
// Continuation.kt:71 resumeWith
// test.kt:10 builder
// test.kt:21 box

// EXPECTATIONS JS_IR
// test.kt:15 box
// test.kt:15 box$slambda
// test.kt:15 box
// test.kt:82 <init>
// test.kt:7 builder
// test.kt:82 <get-context>
// test.kt:82 <get-context>
// test.kt:16 doResume
// test.kt:12 empty
// test.kt:17 doResume
// test.kt:17 box$slambda$slambda
// test.kt:17 doResume
// test.kt:82 <init>
// test.kt:7 builder
// test.kt:82 <get-context>
// test.kt:82 <get-context>
// test.kt:18 doResume
// test.kt:12 empty
// test.kt:19 doResume
// test.kt:82 resumeWith
// test.kt:10 builder
// test.kt:20 doResume
// test.kt:82 resumeWith
// test.kt:10 builder
// test.kt:21 box
