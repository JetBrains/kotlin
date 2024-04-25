// FIR_IDENTICAL
// LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// OPT_IN: kotlin.contracts.ExperimentalContracts
// DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

fun f3(value: String?) {
    if (<!USELESS_IS_CHECK!>!value.isNullOrEmpty() is Boolean<!>) {
        value<!UNSAFE_CALL!>.<!>length
    }
}