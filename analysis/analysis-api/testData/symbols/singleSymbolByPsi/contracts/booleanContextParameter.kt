// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1
// WITH_STDLIB

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
context(a: String?)
fun vali<caret>date(param: Int?) {
    contract {
        returns() implies (a != null)
    }
    a!!
}
