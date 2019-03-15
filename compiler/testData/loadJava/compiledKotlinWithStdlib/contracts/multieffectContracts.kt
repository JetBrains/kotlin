// !LANGUAGE: +AllowContractsForCustomFunctions +ReadDeserializedContracts
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package test

import kotlin.contracts.*

fun twoReturnsValue(b: Boolean) {
    contract {
        returns(true) implies b
        returns(false) implies (!b)
    }
}

fun threeReturnsValue(b: Boolean) {
    contract {
        returnsNotNull() implies (b != null)
        returns(true) implies (b)
        returns(false) implies (!b)
    }
}

fun returnsAndFinished(b: Boolean) {
    contract {
        returns(true) implies (b)
        returns() implies (b != null)
        returns(false) implies (!b)
    }
}

fun returnsAndCalls(b: Boolean, block: () -> Unit) {
    contract {
        returns(false) implies (!b)
        callsInPlace(block)
        returns(true) implies (b)
    }
}

fun severalCalls(x: () -> Unit, y: () -> Unit) {
    contract {
        callsInPlace(x, InvocationKind.AT_MOST_ONCE)
        callsInPlace(y, InvocationKind.AT_LEAST_ONCE)
    }
}