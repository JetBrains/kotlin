// WITH_RUNTIME

// TODO: fix bug in JVM backend and remove this directive
// TARGET_BACKEND: JS

class Controller {
    var result = ""

    suspend fun <T> suspendWithResult(value: T, c: Continuation<T>) {
        result += "["
        c.resume(value)
    }
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>): String {
    val controller = Controller()
    c(controller).resume(Unit)
    return controller.result
}

fun box(): String {
    var value = builder {
        for (v in listOf("A", "B", "C")) {
            when (v) {
                "A" -> result += "A;"
                "B" -> result += suspendWithResult(v) + "]"
                else -> result += suspendWithResult(v) + "]!"
            }
        }
    }
    if (value != "A;[B][C]!") return "fail: suspend as if condition: $value"

    return "OK"
}