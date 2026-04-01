// Currently forbidden, see KT-77175
// LANGUAGE: +AllowContractsOnSomeOperators

import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
operator fun Boolean.plus(x: Boolean): Boolean {
    @Suppress("CONTRACT_NOT_ALLOWED")
    contract { returns() implies (x) }
    return x
}
