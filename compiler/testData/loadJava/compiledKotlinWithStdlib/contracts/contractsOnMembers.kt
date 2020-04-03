// !LANGUAGE: +AllowContractsForCustomFunctions +ReadDeserializedContracts +AllowContractsForNonOverridableMembers
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package test

import kotlin.contracts.*

class Foo {
    fun <X, Y, Z, R> callsEffectWithKind(block: (X, Y, Z) -> R) {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
    }

    fun returnsImplies(x: Boolean) {
        contract { returns() implies (x) }
    }
}