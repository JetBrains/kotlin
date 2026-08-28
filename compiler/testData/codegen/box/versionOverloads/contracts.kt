@file:OptIn(ExperimentalVersionOverloading::class, kotlin.contracts.ExperimentalContracts::class)

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

inline fun contractedVersioned(
    @IntroducedAt("1") value: String? = "OK",
): String {
    contract {
        returns() implies (value != null)
    }
    return value ?: throw IllegalStateException("value must not be null")
}

inline fun contractCallsInPlaceVersioned(
    @IntroducedAt("1") action: () -> String = { "OK" },
): String {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }
    return action()
}

fun box(): String {
    if (contractedVersioned() != "OK") return "FAIL default"
    if (contractedVersioned("K") != "K") return "FAIL explicit"
    if (contractCallsInPlaceVersioned() != "OK") return "FAIL callsInPlace default"
    if (contractCallsInPlaceVersioned { "K" } != "K") return "FAIL callsInPlace explicit"
    return "OK"
}
