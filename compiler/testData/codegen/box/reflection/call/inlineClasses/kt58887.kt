// TARGET_BACKEND: JVM
// WITH_REFLECT
// WITH_COROUTINES

import helpers.EmptyContinuation
import kotlin.coroutines.Continuation
import kotlin.coroutines.startCoroutine
import kotlin.coroutines.suspendCoroutine
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspendBy

var c: Continuation<Z>? = null

suspend fun suspendMe(): Z =
    suspendCoroutine { c = it }

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation())
}

fun box(): String {
    var result = Z("Fail")
    builder {
        val ref: KFunction<Z> = ::suspendMe
        result = ref.callSuspendBy(emptyMap())
    }
    c!!.resumeWith(Result.success(Z("OK")))
    return result.value
}

@JvmInline
value class Z(val value: String) {
    override fun toString(): String = value
}
