// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.contracts.ExperimentalContracts
// Currently forbidden, see KT-77175
// LANGUAGE: +AllowContractsOnSomeOperators

import kotlin.contracts.*

class A(var v: Int = 0)

operator fun A?.set(i: Int, vnew: Int) {
    <!CONTRACT_NOT_ALLOWED!>contract<!> { returns() implies (this@set != null) }
    this!!
    v = vnew
}