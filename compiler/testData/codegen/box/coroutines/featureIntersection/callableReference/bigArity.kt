// !LANGUAGE: +ReleaseCoroutines
// IGNORE_BACKEND: JS_IR, JS, NATIVE, JVM_IR
// WITH_REFLECT
// WITH_COROUTINES

import kotlin.reflect.full.*
import helpers.*
import kotlin.coroutines.*

class A {
    suspend fun foo(
        p00: Long = 0, p01: A = A(), p02: A = A(), p03: A = A(), p04: A = A(), p05: A = A(), p06: A = A(), p07: A = A(), p08: A = A(), p09: A = A(),
        p10: A = A(), p11: A = A(), p12: A = A(), p13: A = A(), p14: A = A(), p15: A = A(), p16: A = A(), p17: A = A(), p18: A = A(), p19: A = A(),
        p20: A = A(), p21: A = A(), p22: A = A(), p23: A = A(), p24: A = A(), p25: A = A(), p26: A = A(), p27: A = A(), p28: A = A(), p29: String
    ): String {
        return p29 + p00
    }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

suspend fun expectsLambdaWithBigArity(c: suspend (Long, Long, Long, Long, Long, Long, Long, Long, Long, Long,
                                                  Long, Long, Long, Long, Long, Long, Long, Long, Long, Long,
                                                  Long, Long, Long, Long, Long, Long, Long, Long, Long, String) -> String): String {
    return c.invoke(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, "OK")
}

fun box(): String {
    val a = A()
    var res = "FAIL 1"
    builder {
        res = A::foo.callSuspend(a, 1L, a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, "OK")
    }
    if (res != "OK1") return res
    res = "FAIL 2"
    builder {
        res = A::foo.callSuspendBy(mapOf(A::foo.parameters.first() to A(), A::foo.parameters.last() to "OK")) as String
    }
    if (res != "OK0") return res
    res = "FAIL 3"
    builder {
        res = expectsLambdaWithBigArity { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, s -> s }
    }
    return res
}
