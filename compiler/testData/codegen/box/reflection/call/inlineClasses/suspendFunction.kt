// TARGET_BACKEND: JVM
// WITH_REFLECT
// WITH_COROUTINES

package test

import kotlin.coroutines.startCoroutine
import kotlin.reflect.full.callSuspend
import helpers.*

inline class Z(val value: String)

class S {
    private var value: Z = Z("")

    suspend fun consumeZ(z: Z) { value = z }
    suspend fun produceZ(): Z = value
    suspend fun consumeAndProduceZ(z: Z): Z = z
}

private fun run0(f: suspend () -> String): String {
    var result = ""
    f.startCoroutine(handleResultContinuation { result = it })
    return result
}

fun box(): String =
    run0 {
        val s = S()
        S::consumeZ.callSuspend(s, Z("z"))
        val v = S::produceZ.callSuspend(s)
        if (v != Z("z")) "Fail: $v"
        else S::consumeAndProduceZ.callSuspend(s, Z("OK")).value
    }
