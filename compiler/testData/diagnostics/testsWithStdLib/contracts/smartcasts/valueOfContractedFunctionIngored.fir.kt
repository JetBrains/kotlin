// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

fun f3(value: String?) {
    if (!value.isNullOrEmpty() is Boolean) {
        value.<!INAPPLICABLE_CANDIDATE!>length<!>
    }
}