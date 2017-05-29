package soSuspendableCallInEndOfLambda

import forTests.builder
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

private fun foo(a: Any) {}

fun main(args: Array<String>) {
    builder {
        foo("Start")
        //Breakpoint!
        run()
    }

    foo("Main end")
    Thread.sleep(100)
}

suspend fun run() {
    suspendCoroutine { cont: Continuation<Unit> ->
        Thread {
            cont.resume(Unit)
            Thread.sleep(10)
        }.start()
    }
}

// STEP_OVER: 2