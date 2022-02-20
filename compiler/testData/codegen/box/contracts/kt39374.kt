// IGNORE_BACKEND: NATIVE
// WITH_STDLIB
// WITH_COROUTINES
import kotlin.contracts.*
import kotlin.coroutines.*
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
