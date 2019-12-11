// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

fun equalsWithVariables(x: Any?, y: Any?) {
    contract {
        returns() implies (x == y)
    }
}

fun identityEqualsWithVariables(x: Any?, y: Any?) {
    contract {
        returns() implies (x === y)
    }
}

fun equalConstants() {
    contract {
        returns() implies (null == null)
    }
}

fun get(): Int? = null
fun equalNullWithCall() {
    contract {
        returns() implies (get() == null)
    }
}