// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

fun equalsWithVariables(x: Any?, y: Any?) {
    contract {
        <!ERROR_IN_CONTRACT_DESCRIPTION!>returns() implies (x == y)<!>
    }
}

fun identityEqualsWithVariables(x: Any?, y: Any?) {
    contract {
        <!ERROR_IN_CONTRACT_DESCRIPTION!>returns() implies (x === y)<!>
    }
}

fun equalConstants() {
    contract {
        <!ERROR_IN_CONTRACT_DESCRIPTION!>returns() implies (null == null)<!>
    }
}

fun get(): Int? = null
fun equalNullWithCall() {
    contract {
        <!ERROR_IN_CONTRACT_DESCRIPTION!>returns() implies (get() == null)<!>
    }
}