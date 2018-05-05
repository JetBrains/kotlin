// !API_VERSION: 1.2
// WITH_RUNTIME
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

suspend fun suspendHere(ctx: CoroutineContext) = suspendCoroutineOrReturn<String> { x ->
    if (x.context == ctx) x.resume("OK") else x.resume("FAIL")
}

@Suppress("DEPRECATION_ERROR")
suspend fun mustBeTailCallOld(): String {
    return suspendHere(kotlin.coroutines.experimental.intrinsics.coroutineContext)
}

suspend fun mustBeTailCallNew(): String {
    return suspendHere(kotlin.coroutines.experimental.coroutineContext)
}

suspend fun retrieveCoroutineContext(): CoroutineContext =
    suspendCoroutineOrReturn { cont -> cont.context }

suspend fun notTailCall(): String {
    return suspendHere(retrieveCoroutineContext())
}