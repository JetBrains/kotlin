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

inline fun <T> myAutoreleasepool(block: () -> T) = autoreleasepool(block)

fun main() {
    autoreleasepool {
        startCoroutine {
            myAutoreleasepool {
                suspendHere()
            }
        }
    } 

    continuation.resume(Unit)
}
