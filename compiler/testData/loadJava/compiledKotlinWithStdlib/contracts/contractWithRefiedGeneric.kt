// LANGUAGE: +AllowContractsForCustomFunctions +ReadDeserializedContracts +AllowContractsForNonOverridableMembers +AllowReifiedGenericsInContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package test

import kotlin.contracts.*

inline fun <reified T> requireIsInstance(value: Any?) {
    contract {
        returns() implies (value is T)
    }
    if (value !is T) {
        throw IllegalArgumentException()
    }
}

inline fun <reified T, reified U> cast(value: Any?, z: U): T {
    contract {
        returns() implies (value is T)
    }
    return value as T
}
