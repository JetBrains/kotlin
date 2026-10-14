// FREE_COMPILER_ARGS: -Xbinary=genericSafeCasts=true
// WITH_STDLIB
// KT-89267
// IGNORE_BACKEND: JS_IR, JS_IR_ES6

import kotlin.coroutines.*

private interface Bindings {
    val value: String
}

private fun <T : Any> invokeIt(any: Any, extract: (T) -> String): String {
    @Suppress("UNCHECKED_CAST")
    return extract(any as T)
}

private fun <T : Any> invokeExtension(any: Any, extract: T.() -> String): String {
    @Suppress("UNCHECKED_CAST")
    return (any as T).extract()
}

private fun <T : Any> invokeSecond(any: Any, extract: (Int, T) -> String): String {
    @Suppress("UNCHECKED_CAST")
    return extract(42, any as T)
}

private suspend fun <T : Any> invokeSuspend(any: Any, extract: suspend (T) -> String): String {
    @Suppress("UNCHECKED_CAST")
    return extract(any as T)
}

private inline fun catchesClassCastException(block: () -> Unit): Boolean =
    try {
        block()
        false
    } catch (_: ClassCastException) {
        true
    }

fun typeParameterLambdaArgument(): String? {
    var caught: String? = null
    try {
        invokeIt<Bindings>("wrong-type") { it.value }
    } catch (t: Throwable) {
        caught = t::class.simpleName
    }
    return caught
}

private fun extensionLambdaReceiver(): Boolean = catchesClassCastException {
    invokeExtension<Bindings>("wrong-type") { value }
}

private fun secondLambdaArgument(): Boolean = catchesClassCastException {
    invokeSecond<Bindings>("wrong-type") { _, bindings -> bindings.value }
}

private fun suspendLambdaArgument(): Boolean {
    var caught = false
    var completionFailure: Throwable? = null

    suspend {
        try {
            invokeSuspend<Bindings>("wrong-type") { it.value }
        } catch (_: ClassCastException) {
            caught = true
        }
    }.startCoroutine(Continuation(EmptyCoroutineContext) { result ->
        completionFailure = result.exceptionOrNull()
    })

    completionFailure?.let { throw it }
    return caught
}

fun box(): String {
    if (typeParameterLambdaArgument() != "ClassCastException") return "FAIL: ordinary lambda argument"
    if (!extensionLambdaReceiver()) return "FAIL: extension lambda receiver"
    if (!secondLambdaArgument()) return "FAIL: second lambda argument"
    if (!suspendLambdaArgument()) return "FAIL: suspend lambda argument"
    return "OK"
}
