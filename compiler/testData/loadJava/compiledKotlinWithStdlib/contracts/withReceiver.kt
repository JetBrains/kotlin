// LANGUAGE: +AllowContractsForCustomFunctions +ReadDeserializedContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts

package test

import kotlin.contracts.*

fun Any?.isNotNull(): Boolean {
    contract {
        returns(true) implies (this@isNotNull != null)
    }
    return this != null
}