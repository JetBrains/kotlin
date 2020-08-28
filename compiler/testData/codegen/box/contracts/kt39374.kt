// IGNORE_BACKEND: NATIVE
// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST

import kotlin.contracts.*
import COROUTINES_PACKAGE.*
import helpers.*

@ExperimentalContracts
public fun <T> runBlocking(block: suspend () -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    var res: T? = null
    suspend {
        res = block()
    }.startCoroutine(EmptyContinuation)
    return res!!
}

sealed class S {
    class Z : S() {
        fun f(): String = "OK"
    }
}

val z: S = S.Z()

@ExperimentalContracts
fun box(): String = when (val w = z) {
    is S.Z -> runBlocking { w.f() }
}