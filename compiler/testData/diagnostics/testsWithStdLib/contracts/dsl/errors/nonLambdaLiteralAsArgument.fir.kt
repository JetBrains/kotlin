// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER -EXPOSED_PARAMETER_TYPE

import kotlin.contracts.*

fun passLambdaValue(l: ContractBuilder.() -> Unit) {
    contract(l)
}

fun passAnonymousFunction(x: Boolean) {
    contract(fun ContractBuilder.() {
        <!UNRESOLVED_REFERENCE!>returns<!>() <!UNRESOLVED_REFERENCE!>implies<!> x
    })
}