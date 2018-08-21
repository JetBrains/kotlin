// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

fun equalsWithVariables(x: Any?, y: Any?) {
    contract {
        returns() implies (<!ERROR_IN_CONTRACT_DESCRIPTION(only equality comparisons with 'null' allowed)!>x == y<!>)
    }
}

fun identityEqualsWithVariables(x: Any?, y: Any?) {
    contract {
        returns() implies (<!ERROR_IN_CONTRACT_DESCRIPTION(only equality comparisons with 'null' allowed)!>x === y<!>)
    }
}

fun equalConstants() {
    contract {
        returns() implies (<!SENSELESS_COMPARISON, ERROR_IN_CONTRACT_DESCRIPTION(only equality comparisons with 'null' allowed)!>null == null<!>)
    }
}

fun get(): Int? = null
fun equalNullWithCall() {
    contract {
        returns() implies (<!ERROR_IN_CONTRACT_DESCRIPTION(only references to parameters are allowed in contract description)!>get()<!> == null)
    }
}