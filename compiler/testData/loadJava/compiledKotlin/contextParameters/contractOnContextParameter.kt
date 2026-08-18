// LANGUAGE: +ContextParameters

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
