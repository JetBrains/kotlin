// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*


class Controller {
    var result = ""

    suspend fun <T> suspendWithResult(value: T): T = suspendCoroutineUninterceptedOrReturn { c ->
        c.resume(value)
        COROUTINE_SUSPENDED
    }
}

fun Controller.consumeCancel(c: Throwable?) {
    result += if (c == null) "?" else "!"
}

fun newIterator() = Iterator()

class Iterator() {
    var hasNextX = true
    public suspend fun hasNext(): Boolean {
        val tmp = hasNextX
        hasNextX = false
        return tmp
    }
    public suspend fun next(): String = "OK"
}

public inline fun Controller.consume(action: Controller.() -> String?): String? {
    var cause: Throwable? = null
    try {
        return action()
    } catch(x: Exception) {
        cause = x
        throw x
    } finally {
        consumeCancel(cause)
    }
}

public suspend fun Controller.doTest(): String? {
    return consume {
        val iterator = newIterator()
        if (!iterator.hasNext())
            return null
        return iterator.next()
    }
}


fun builder(c: suspend Controller.() -> Unit): String {
    val controller = Controller()
    c.startCoroutine(controller, EmptyContinuation)
    return controller.result
}

fun Controller.add(s: String?) {
    result += s
}

fun box(): String {
    val value = builder {
        add(doTest())
    }
    return if (value != "?OK") return "Fail: $value" else "OK"
}
