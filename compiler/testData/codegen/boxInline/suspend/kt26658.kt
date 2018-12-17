// IGNORE_BACKEND: JVM_IR
// FILE: inlined.kt
// WITH_RUNTIME
// NO_CHECK_LAMBDA_INLINING
import kotlin.coroutines.*

class Controller(val s: String)

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(object : Continuation<Unit>{
        override fun resumeWith(result: Result<Unit>) {
            result.getOrThrow()
        }

        override val context: CoroutineContext
            get() = EmptyCoroutineContext

    })
}

inline fun execute(crossinline action: suspend () -> Unit) {
    builder { action() }
}

fun builder(controller: Controller, c: suspend Controller.() -> Unit) {
    c.startCoroutine(controller, object : Continuation<Unit>{
        override fun resumeWith(result: Result<Unit>) {
            result.getOrThrow()
        }

        override val context: CoroutineContext
            get() = EmptyCoroutineContext
    })
}

inline fun execute(controller: Controller, crossinline action: suspend Controller.() -> Unit) {
    builder(controller) { action() }
}

// FILE: inlineSite.kt
import kotlin.coroutines.*

suspend fun withDefaultParameter(s: String, s1: String = "") = s + s1

fun launch(s: String = "", block: suspend () -> String): String {
    var res = s
    builder { res += block() }
    return res
}

inline fun launchCrossinline(s: String = "", crossinline block: suspend () -> String): String {
    var res = s
    builder { res += block() }
    return res
}

suspend fun Controller.withDefaultParameter(s: String, s1: String = "") = this.s + s + s1

fun Controller.launch(s: String = "", block: suspend Controller.() -> String): String {
    var res = s
    builder(this) { res += block() }
    return res
}

inline fun Controller.launchCrossinline(s: String = "", crossinline block: suspend Controller.() -> String): String {
    var res = s
    builder(this) { res += block() }
    return res
}

fun box(): String {
    var res = ""
    execute {
        res = withDefaultParameter("OK")
    }
    if (res != "OK") return "FAIL 1: $res"
    execute {
        res = withDefaultParameter("O", "K")
    }
    if (res != "OK") return "FAIL 2: $res"
    execute {
        res = launch {
            withDefaultParameter("OK")
        }
    }
    if (res != "OK") return "FAIL 3: $res"
    execute {
        res = launch {
            withDefaultParameter("O", "K")
        }
    }
    if (res != "OK") return "FAIL 4: $res"
    execute {
        res = launch("O") {
            withDefaultParameter("K")
        }
    }
    if (res != "OK") return "FAIL 5: $res"
    execute {
        res = launch("O") {
            withDefaultParameter("", "K")
        }
    }
    if (res != "OK") return "FAIL 6: $res"
    execute {
        res = launchCrossinline {
            withDefaultParameter("OK")
        }
    }
    if (res != "OK") return "FAIL 7: $res"
    execute {
        res = launchCrossinline {
            withDefaultParameter("O", "K")
        }
    }
    if (res != "OK") return "FAIL 8: $res"
    execute {
        res = launchCrossinline ("O") {
            withDefaultParameter("K")
        }
    }
    if (res != "OK") return "FAIL 9: $res"
    execute {
        res = launchCrossinline("O") {
            withDefaultParameter("", "K")
        }
    }
    if (res != "OK") return "FAIL 10: $res"

    val controller = Controller("A")
    execute(controller) {
        res = withDefaultParameter("OK")
    }
    if (res != "AOK") return "FAIL 11: $res"
    execute(controller) {
        res = withDefaultParameter("O", "K")
    }
    if (res != "AOK") return "FAIL 12: $res"
    execute(controller) {
        res = launch {
            withDefaultParameter("OK")
        }
    }
    if (res != "AOK") return "FAIL 13: $res"
    execute(controller) {
        res = launch {
            withDefaultParameter("O", "K")
        }
    }
    if (res != "AOK") return "FAIL 14: $res"
    execute(controller) {
        res = launch("O") {
            withDefaultParameter("K")
        }
    }
    if (res != "OAK") return "FAIL 15: $res"
    execute(controller) {
        res = launch("O") {
            withDefaultParameter("", "K")
        }
    }
    if (res != "OAK") return "FAIL 16: $res"
    execute(controller) {
        res = launchCrossinline {
            withDefaultParameter("OK")
        }
    }
    if (res != "AOK") return "FAIL 17: $res"
    execute(controller) {
        res = launchCrossinline {
            withDefaultParameter("O", "K")
        }
    }
    if (res != "AOK") return "FAIL 18: $res"
    execute(controller) {
        res = launchCrossinline ("O") {
            withDefaultParameter("K")
        }
    }
    if (res != "OAK") return "FAIL 19: $res"
    execute(controller) {
        res = launchCrossinline("O") {
            withDefaultParameter("", "K")
        }
    }
    if (res != "OAK") return "FAIL 20: $res"
    return "OK"
}
