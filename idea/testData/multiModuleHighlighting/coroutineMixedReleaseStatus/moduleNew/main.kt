import libN.*
import libO.*

suspend fun newMain() {
    newFoo()
    oldFoo(<error descr="[NO_VALUE_FOR_PARAMETER] No value passed for parameter 'continuation'">)</error>

    oldFoo(
        object : kotlin.coroutines.experimental.Continuation<Unit> {
            override val context
                get() = null!!

            override fun resume(value: Unit) {}

            override fun resumeWithException(exception: Throwable) {}
        }
    )

    // TODO: actually, it's a bug
    oldMain()
}

fun newMain2() {
    newBuilder {
        newMain()
    }

    oldBuilder {
        <error descr="[ILLEGAL_SUSPEND_FUNCTION_CALL] Suspend function 'newMain' should be called only from a coroutine or another suspend function">newMain</error>()

        // `suspend () -> Unit` becomes (Continuation<Unit> -> Any?)
        it.resume(Unit)
    }
}
