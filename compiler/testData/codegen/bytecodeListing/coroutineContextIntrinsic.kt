// WITH_RUNTIME
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

suspend fun suspendHere(ctx: CoroutineContext) = suspendCoroutineOrReturn<String> { x ->
    if (x.context == ctx) x.resume("OK") else x.resume("FAIL")
}

suspend fun mustBeTailCall(): String {
    return suspendHere(coroutineContext)
}

suspend fun retrieveCoroutineContext(): CoroutineContext =
        suspendCoroutineOrReturn { cont -> cont.context }

suspend fun notTailCall(): String {
    return suspendHere(retrieveCoroutineContext())
}