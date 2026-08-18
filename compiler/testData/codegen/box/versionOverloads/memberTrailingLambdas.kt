// WITH_STDLIB
// WITH_COROUTINES
@file:OptIn(ExperimentalVersionOverloading::class)

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

class TrailingLambdaOwner {
    fun transform(
        value: String = "O",
        @IntroducedAt("1") suffix: String = "K",
        transform: (String) -> String,
    ): String = transform(value + suffix)

    fun String.extensionTransform(
        value: String = "O",
        @IntroducedAt("1") suffix: String = "K",
        transform: (String) -> String,
    ): String = transform(this + value + suffix)

    suspend fun suspendTransform(
        value: String = "O",
        @IntroducedAt("1") suffix: String = "K",
        transform: suspend (String) -> String,
    ): String = transform(value + suffix)
}

fun runTrailingSuspend(block: suspend () -> String): String {
    var result: Result<String>? = null
    block.startCoroutine(Continuation(EmptyCoroutineContext) { result = it })
    return result!!.getOrThrow()
}

fun box(): String {
    val owner = TrailingLambdaOwner()
    if (owner.transform { it } != "OK") return "FAIL member default"
    if (owner.transform("A", "B") { it } != "AB") return "FAIL member explicit"
    with(owner) {
        if ("X".extensionTransform { it } != "XOK") return "FAIL extension default"
        if ("X".extensionTransform("A", "B") { it } != "XAB") return "FAIL extension explicit"
    }
    if (runTrailingSuspend { owner.suspendTransform { it } } != "OK") return "FAIL suspend default"
    if (runTrailingSuspend { owner.suspendTransform("A", "B") { it } } != "AB") return "FAIL suspend explicit"
    return "OK"
}
