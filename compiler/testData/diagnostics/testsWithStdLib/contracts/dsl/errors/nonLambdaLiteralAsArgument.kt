// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER -EXPOSED_PARAMETER_TYPE

import kotlin.contracts.*

fun passLambdaValue(l: ContractBuilder.() -> Unit) {
    contract(<!ERROR_IN_CONTRACT_DESCRIPTION(first argument of 'contract'-call should be a lambda expression)!>l<!>)
}

fun passAnonymousFunction(x: Boolean) {
    contract(<!ERROR_IN_CONTRACT_DESCRIPTION(first argument of 'contract'-call should be a lambda expression)!>fun ContractBuilder.() {
        returns() implies x
    }<!>)
}