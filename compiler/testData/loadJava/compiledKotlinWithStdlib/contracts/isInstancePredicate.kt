// !LANGUAGE: +AllowContractsForCustomFunctions +ReadDeserializedContracts
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package test

import kotlin.contracts.*

class A

fun simpleIsInstace(x: Any?) {
    contract {
        returns(true) implies (x is A)
    }
}

fun Any?.receiverIsInstance() {
    contract {
        returns(true) implies (this@receiverIsInstance is A)
    }
}
