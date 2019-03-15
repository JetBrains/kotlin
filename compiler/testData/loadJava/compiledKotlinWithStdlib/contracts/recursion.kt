// !LANGUAGE: +AllowContractsForCustomFunctions +ReadDeserializedContracts
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package test

import kotlin.contracts.*

fun foo(n: Int, x: Any?): Boolean {
    contract {
        returns(true) implies (x is String)
    }
    return if (n == 0) x is String else foo(n - 1, x)
}