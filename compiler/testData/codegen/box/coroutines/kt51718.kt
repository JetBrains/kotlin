// WITH_STDLIB

import kotlin.coroutines.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        it.getOrThrow()
    })
}

class Service {
    suspend fun getCanalConfig() {
        var start: Start? = null
        var configuration: String? = null
        val starts: Array<Start>? = null
        if (starts != null && starts.isNotEmpty()) {
            start = starts[0]
            configuration = call2()
        }
        //start = start as Start // kotlin 1.5.0 need for produce correct bytecode
        call(start!!)
    }

    val canalConfig = suspend {
        var start: Start? = null
        var configuration: String? = null
        val starts: Array<Start>? = null
        if (starts != null && starts.isNotEmpty()) {
            start = starts[0]
            configuration = call2()
        }
        //start = start as Start // kotlin 1.5.0 need for produce correct bytecode
        call(start!!)
    }

    fun call(start: Start) {

    }

    suspend fun call2(
    ): String? {
        return null
    }
}

class Start()

fun box(): String {
    builder {
        try {
            Service().getCanalConfig()
            error("FAIL 1")
        } catch (ignored: NullPointerException) {}
        try {
            Service().canalConfig()
            error("FAIL 2")
        } catch (ignored: NullPointerException) {}
    }
    return "OK"
}