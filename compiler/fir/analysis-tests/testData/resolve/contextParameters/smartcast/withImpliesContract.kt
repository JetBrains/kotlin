// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-74572
// LANGUAGE: +ContextParameters
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

context(a: String?)
@ExperimentalContracts
fun validate() {
    contract {
        <!ERROR_IN_CONTRACT_DESCRIPTION!>returns() implies (a!= null)<!>
    }
}

context(a: String?)
@ExperimentalContracts
fun process() {
    validate()
    a<!UNSAFE_CALL!>.<!>length
}