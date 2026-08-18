// WITH_STDLIB
// WITH_COROUTINES
@file:OptIn(ExperimentalVersionOverloading::class)

import kotlin.coroutines.startCoroutine

class GenericPairFactory<C>(private val container: C) {
    fun <T> create(value: T, @IntroducedAt("1") fallback: T = value): Pair<C, T> = container to fallback
}

inline fun <reified T> reifiedIdentity(
    value: T,
    @IntroducedAt("1") fallback: T = value,
): T = fallback

suspend fun <T> suspendIdentity(
    value: T,
    @IntroducedAt("1") fallback: T = value,
): T = fallback

tailrec fun <T> tailrecIdentity(
    value: T,
    @IntroducedAt("1") fallback: T = value,
    @IntroducedAt("1") remaining: Int = 2,
): T = if (remaining == 0) fallback else tailrecIdentity(value, fallback, remaining - 1)

fun <T> runSuspendIdentity(block: suspend () -> T): T {
    var result: Result<T>? = null
    block.startCoroutine(kotlin.coroutines.Continuation(kotlin.coroutines.EmptyCoroutineContext) { result = it })
    return result!!.getOrThrow()
}

fun <T> identityWithIntroducedFallback(value: T, @IntroducedAt("1") fallback: T = value): T = fallback

fun box(): String {
    val factory = GenericPairFactory("prefix")
    if (factory.create(1) != ("prefix" to 1)) return "FAIL member generic return"
    if (factory.create(1, 2) != ("prefix" to 2)) return "FAIL member generic explicit"

    if (reifiedIdentity<String>("O") != "O") return "FAIL reified default"
    if (reifiedIdentity("O", "K") != "K") return "FAIL reified explicit"

    if (runSuspendIdentity { suspendIdentity("O") } != "O") return "FAIL suspend default"
    if (runSuspendIdentity { suspendIdentity("O", "K") } != "K") return "FAIL suspend explicit"

    if (tailrecIdentity("O") != "O") return "FAIL tailrec default"
    if (tailrecIdentity("O", "K") != "K") return "FAIL tailrec explicit"

    if (identityWithIntroducedFallback("OK") != "OK") return "fail identityWithIntroducedFallback"

    return "OK"
}
