// WITH_STDLIB
// LANGUAGE: +SuspendFunctionAsSupertype
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// ^^^ JS target doesn't support both function and suspend function types as supertypes

import kotlin.coroutines.*

var failure: String? = "FAIL ILLEGAL STATE"

class SuspendNoneUnit: suspend () -> Unit {
    override suspend fun invoke() {
        failure = null
    }
}

class SuspendIntString: suspend (Int) -> String {
    override suspend fun invoke(p: Int): String {
        failure = if (p == 7) null else "FAIL CONDITION"
        return "OK"
    }
}

fun suspendNoneUnit(): String? {
    failure = "FAIL INHERITED 2"
    val a = suspend {
        val snu = SuspendNoneUnit()
        snu()
    }
    a.startCoroutine(Continuation(EmptyCoroutineContext) { it.getOrThrow() })
    return failure
}

fun suspendIntString(): String? {
    failure = "FAIL INHERITED 3"
    val a = suspend {
        val sis = SuspendIntString()
        sis(7)
    }
    a.startCoroutine(Continuation(EmptyCoroutineContext) { it.getOrThrow() })
    return failure
}

fun box(): String {
    val failures = listOfNotNull(
        suspendNoneUnit(),
        suspendIntString()
    )

    return if (failures.isNotEmpty()) failures.joinToString("\n") else "OK"
}
