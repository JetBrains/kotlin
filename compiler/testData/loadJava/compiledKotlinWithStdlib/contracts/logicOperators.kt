// !LANGUAGE: +AllowContractsForCustomFunctions +ReadDeserializedContracts
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package test

import kotlin.contracts.*


fun orSequence(x: Any?, y: Any?, b: Boolean) {
    contract {
        returns() implies (x is String || y is Int || !b)
    }
}

class A
class B

fun andSequence(x: Any?, y: Any?, b:Boolean) {
    contract {
        returns() implies (x is A && x is B && ((y is A) && b))
    }
}