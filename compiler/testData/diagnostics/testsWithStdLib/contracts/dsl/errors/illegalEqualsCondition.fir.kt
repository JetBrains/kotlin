// LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// OPT_IN: kotlin.contracts.ExperimentalContracts
// DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

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
        <!ERROR_IN_CONTRACT_DESCRIPTION!>returns() implies (<!SENSELESS_COMPARISON!>null == null<!>)<!>
    }
}

fun get(): Int? = null
fun equalNullWithCall() {
    contract {
        <!ERROR_IN_CONTRACT_DESCRIPTION!>returns() implies (get() == null)<!>
    }
}
