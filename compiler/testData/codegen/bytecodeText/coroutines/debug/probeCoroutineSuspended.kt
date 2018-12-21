// IGNORE_BACKEND: JVM_IR
// TREAT_AS_ONE_FILE

import kotlin.coroutines.intrinsics.*
import kotlin.coroutines.*

suspend fun suspended() = suspendCoroutineUninterceptedOrReturn<Int> { cont ->
    cont.resume(0)
    COROUTINE_SUSPENDED
}

suspend fun simpleReturn() = suspendCoroutineUninterceptedOrReturn<Int> { cont ->
    cont.resume(0)
}

// 2 INVOKESTATIC kotlin/coroutines/jvm/internal/DebugProbesKt.probeCoroutineSuspended
