// KT-66095: java.lang.ClassCastException: SuspendConversionKt$box$f$1 cannot be cast to kotlin.jvm.functions.Function1
// WITH_STDLIB
// WITH_COROUTINES

import kotlin.test.*

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

open class EmptyContinuation(override val context: CoroutineContext = EmptyCoroutineContext) : Continuation<Any?> {
    companion object : EmptyContinuation()
    override fun resumeWith(result: Result<Any?>) { result.getOrThrow() }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = 0

    val f: () -> Unit = { result = 42 }
    builder(f)

    assertEquals(42, result)

    return "OK"
}
