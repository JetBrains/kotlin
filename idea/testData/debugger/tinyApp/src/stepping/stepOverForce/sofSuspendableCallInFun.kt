package sofSuspendableCallInFun

import forTests.builder
import forTests.WaitFinish
import kotlin.coroutines.Continuation
import kotlin.coroutines.*

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
    //Breakpoint!
    run()
    foo("End")
}

suspend fun run() {
    //Breakpoint!
    foo("This breakpoint should be skipped")

    suspendCoroutine { cont: Continuation<Unit> ->
        Thread {
            Thread.sleep(10)
            cont.resume(Unit)
            waiter.finish()
        }.start()
    }
}

// STEP_OVER_FORCE: 2