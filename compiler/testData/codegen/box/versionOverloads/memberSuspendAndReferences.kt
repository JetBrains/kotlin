// WITH_STDLIB
// WITH_COROUTINES
@file:OptIn(ExperimentalVersionOverloading::class)

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

class SuspendVersionedHolder {
    suspend fun member(
        value: String = "O",
        @IntroducedAt("1") suffix: String = "K",
    ): String = value + suffix
}

suspend fun String.suspendExtension(
    @IntroducedAt("1") suffix: String = "K",
): String = this + suffix

fun <T> runSuspendVersioned(block: suspend () -> T): T {
    var result: Result<T>? = null
    block.startCoroutine(Continuation(EmptyCoroutineContext) { result = it })
    return result!!.getOrThrow()
}

fun box(): String {
    val holder = SuspendVersionedHolder()
    if (runSuspendVersioned { holder.member() } != "OK") return "FAIL member default"
    if (runSuspendVersioned { with(holder) { "A".suspendExtension() } } != "AK") return "FAIL extension default"

    val memberReference: suspend (SuspendVersionedHolder, String, String) -> String =
        SuspendVersionedHolder::member
    if (runSuspendVersioned { memberReference(holder, "O", "K") } != "OK") return "FAIL member reference"

    val extensionReference: suspend (String, String) -> String = String::suspendExtension
    if (runSuspendVersioned { extensionReference("O", "K") } != "OK") return "FAIL extension reference"

    return "OK"
}
