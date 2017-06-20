package soSuspendableCallInEndOfFun

import forTests.builder
import forTests.WaitFinish
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

private fun foo(a: Any) {}

val waiter = WaitFinish()

fun main(args: Array<String>) {
    builder {
        inFun()
    }

    foo("Main end")
    waiter.waitEnd()
}

suspend fun inFun() {
    foo("Start")
    //Breakpoint!
    run()
}

suspend fun run() {
    suspendCoroutine { cont: Continuation<Unit> ->
        Thread {
            Thread.sleep(10)
            cont.resume(Unit)
            waiter.finish()
        }.start()
    }
}

// STEP_OVER: 4