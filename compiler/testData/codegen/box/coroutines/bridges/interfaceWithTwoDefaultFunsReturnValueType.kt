// Corresponds to KT-79442

// TARGET_BACKEND: JVM

// WITH_STDLIB
// WITH_COROUTINES
// JVM_DEFAULT_MODE: enable

// CHECK_BYTECODE_LISTING
// CHECK_BYTECODE_TEXT

import helpers.*
import kotlin.coroutines.*

interface Repository {
    suspend fun get(): Result<String>
    suspend fun local(): Result<String> = get()
    suspend fun remote(): Result<String> = get()

}

fun box(): String {

    var res = "Fail 1"

    val x = object : Repository {
        override suspend fun get(): Result<String> {
            return Result.success("OK")
        }
    }

    suspend {
        res = x.local().getOrDefault("Fail 2")
    }.startCoroutine(EmptyContinuation)

    return res
}

// @Repository$DefaultImpls.class:
// 0 @Lkotlin/coroutines/jvm/internal/DebugMetadata;
// 0 L$0
// 2 ALOAD 1