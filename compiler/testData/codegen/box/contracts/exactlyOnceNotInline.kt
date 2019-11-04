// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect +ReadDeserializedContracts
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// IGNORE_BACKEND: NATIVE

import kotlin.contracts.*

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
public fun myrun(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block()
}

fun box(): String {
    val x: Long
    myrun {
        x = 1L
    }
    return if (x != 1L) "FAIL" else "OK"
}
