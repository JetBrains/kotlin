// TARGET_BACKEND: JVM
// WITH_REFLECT
// WITH_COROUTINES
// LANGUAGE: +CompanionBlocks +CompanionExtensions

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.callSuspendBy
import kotlin.test.assertEquals

suspend fun suspendHere(): String = suspendCoroutineUninterceptedOrReturn { x ->
    x.resume("s")
    COROUTINE_SUSPENDED
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

class A {
    companion {
        suspend fun f(x: Int): String = suspendHere() + x
    }
}

interface I {
    companion {
        suspend fun g(x: Int = 2): String = suspendHere() + x
    }
}

suspend companion fun A.ext(x: Int = 3): String = suspendHere() + x

fun box(): String {
    var result = ""
    builder {
        result = (A::f.callSuspend(1) as String) + I::g.callSuspendBy(emptyMap()) + A::ext.callSuspendBy(emptyMap())
    }
    assertEquals("s1s2s3", result)
    return "OK"
}
