// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*

val traceResult = StringBuilder()

fun trace(o : Any) {
    traceResult.append(o)
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

class Test {
    suspend fun someSuspendFun(): Unit = trace("S")

    fun test(): Unit {
        builder {
            1F.also { trace(it) }
            try {
                someSuspendFun()
            } catch (throwable: Throwable) {
                someSuspendFun()
                throw throwable
            } finally {
                2.also { trace(it) }
            }
        }
    }
}

fun box(): String {
    Test().test()
    return if (traceResult.toString() == "" + 1F + "S" + 2) "OK" else "FAIL: $traceResult"
}
