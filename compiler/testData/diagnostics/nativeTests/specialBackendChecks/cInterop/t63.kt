// FIR_IDENTICAL
import kotlinx.cinterop.*
import kotlin.coroutines.*

open class EmptyContinuation(override val context: CoroutineContext = EmptyCoroutineContext) : Continuation<Any?> {
    companion object : EmptyContinuation()
    override fun resumeWith(result: Result<Any?>) { result.getOrThrow() }
}

lateinit var continuation: Continuation<Unit>

suspend fun suspendHere(): Unit = suspendCoroutine { cont ->
    continuation = cont
}


fun startCoroutine(block: suspend () -> Unit) {
    block.startCoroutine(EmptyContinuation)
}

@OptIn(kotlinx.cinterop.BetaInteropApi::class)
fun main() {
    autoreleasepool {
        startCoroutine {
            autoreleasepool {
                suspendHere()
            }
        }
    } 

    continuation.resume(Unit)
}
