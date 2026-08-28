// WITH_STDLIB
// WITH_COROUTINES
@file:OptIn(ExperimentalVersionOverloading::class)

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

suspend fun versionedSuspend(
    value: String = "O",
    @IntroducedAt("1") suffix: String = "K",
    @IntroducedAt("2") punctuation: String = "!",
): String = value + suffix + punctuation

fun runVersionedSuspend(block: suspend () -> String): String {
    var result: Result<String>? = null
    block.startCoroutine(Continuation(EmptyCoroutineContext) { result = it })
    return result!!.getOrThrow()
}

fun box(): String {
    if (runVersionedSuspend { versionedSuspend() } != "OK!") return "FAIL default"
    if (runVersionedSuspend { versionedSuspend("A", "B") } != "AB!") return "FAIL second"
    if (runVersionedSuspend { versionedSuspend("A", "B", "C") } != "ABC") return "FAIL all"
    return "OK"
}
