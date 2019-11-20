package Hello

import kotlin.concurrent.thread
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.resume
import kotlin.reflect.jvm.javaMethod

@kotlin.jvm.Volatile
private var result = ""
@kotlin.jvm.Volatile
private var callback: Function0<Unit>? = null

suspend fun appendAndSuspend(s: String) {
    result += s

    suspendCoroutine<Unit> { continuation ->
        callback = {
            continuation.resume(Unit)
        }
    }
}

suspend fun main() {
    thread(isDaemon = true) {
        while (true) {
            val c = callback
            c?.invoke()
            Thread.sleep(500)
        }
    }

    appendAndSuspend("O")
    appendAndSuspend("K")
    println(result)
    callback = null
}
