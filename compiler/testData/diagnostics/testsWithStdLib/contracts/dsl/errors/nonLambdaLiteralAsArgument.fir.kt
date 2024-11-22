// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// OPT_IN: kotlin.contracts.ExperimentalContracts
// DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER -EXPOSED_PARAMETER_TYPE

import kotlin.contracts.*

fun passLambdaValue(l: ContractBuilder.() -> Unit) {
    <!CONTRACT_NOT_ALLOWED("Contract should be the first statement.")!>contract<!>(l)
}

fun passAnonymousFunction(x: Boolean) {
    <!ERROR_IN_CONTRACT_DESCRIPTION!>contract(fun ContractBuilder.() {
        returns() implies x
    })<!>
}

// Check combined behaviour when the contract is both ill-formed and on
// a function that does not allow contracts.
// TODO: (KT-72772) it may be clearer to generate both errors here.
open class OpenClass {
    open fun passLambdaValue(l: ContractBuilder.() -> Unit) {
        <!CONTRACT_NOT_ALLOWED!>contract<!>(l)
    }
}
