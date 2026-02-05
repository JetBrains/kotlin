// LANGUAGE: +WrapContinuationForTailCallFunctions
// ISSUE: KT-83363

// WITH_STDLIB
// TARGET_BACKEND: JVM

import kotlin.coroutines.*

class BuildOptions

suspend fun compile(makeZip: Boolean = false, options: BuildOptions) {}

suspend fun doTest() {
    compile(
        options = BuildOptions()
    )
}

fun box(): String {
    suspend {
        doTest()
    }.startCoroutine(Continuation(EmptyCoroutineContext) {
        it.getOrThrow()
    })
    return "OK"
}