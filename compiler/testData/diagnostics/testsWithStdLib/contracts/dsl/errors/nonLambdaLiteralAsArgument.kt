// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// OPT_IN: kotlin.contracts.ExperimentalContracts
// DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER -EXPOSED_PARAMETER_TYPE

import kotlin.contracts.*

fun passLambdaValue(l: ContractBuilder.() -> Unit) {
    contract(<!ERROR_IN_CONTRACT_DESCRIPTION("first argument of 'contract'-call should be a lambda expression")!>l<!>)
}

fun passAnonymousFunction(x: Boolean) {
    contract(<!ERROR_IN_CONTRACT_DESCRIPTION("first argument of 'contract'-call should be a lambda expression")!>fun ContractBuilder.() {
        returns() implies x
    }<!>)
}

// Check combined behaviour when the contract is both ill-formed and on
// a function that does not allow contracts.
// TODO: (KT-72772) it may be clearer to generate both errors here.
open class OpenClass {
    open fun passLambdaValue(l: ContractBuilder.() -> Unit) {
        <!CONTRACT_NOT_ALLOWED!>contract<!>(l)
    }
}
