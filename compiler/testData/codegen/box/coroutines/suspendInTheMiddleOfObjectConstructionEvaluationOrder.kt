// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: NESTED_OBJECT_INIT
// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

class Controller {
    suspend fun suspendHere(): String = suspendCoroutineUninterceptedOrReturn { x ->
        x.resume("K")
        COROUTINE_SUSPENDED
    }
}

fun builder(c: suspend Controller.() -> Unit) {
    c.startCoroutine(Controller(), EmptyContinuation)
}

val logger = StringBuilder()

class A(val first: String, val second: String) {
    init {
        logger.append("A.<init>;")
    }

    override fun toString() = "$first$second"

    companion object {
        init {
            logger.append("A.<clinit>;")
        }
    }
}

inline fun <T> logged(message: String, result: () -> T): T {
    logger.append(message)
    return result()
}

fun box(): String {
    var result = "OK"

    builder {
        var local: Any = A(logged("args;") { "O" }, suspendHere())

        if (local.toString() != "OK") {
            result = "fail 1: $local"
            return@builder
        }
    }

    if (logger.toString() != "args;A.<clinit>;A.<init>;") {
        return "Fail: '$logger'"
    }

    return result
}
