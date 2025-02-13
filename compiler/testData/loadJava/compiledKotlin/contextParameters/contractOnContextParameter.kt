// LANGUAGE: +ContextParameters
// IGNORE_FIR_METADATA_LOADING_K1
// IGNORE_BACKEND_K1: ANY
// IGNORE K1
package test

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

context(a: String?)
@OptIn(ExperimentalContracts::class)
fun validate() {
    contract {
        returns() implies (a!= null)
    }
}