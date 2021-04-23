// !LANGUAGE: +AllowContractsForNonOverridableMembers +AllowReifiedGenericsInContracts
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -UNREACHABLE_CODE -UNUSED_EXPRESSION
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

// TESTCASE NUMBER: 1
object case_1 {
    val value_1 = getBoolean()
    const val value_2 = true
    private const val value_3 = false

    fun case_1_1(): Boolean? {
        contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>returnsNotNull() implies (<!UNRESOLVED_REFERENCE!>value_1<!>)<!> }
        return if (value_1) true else null
    }
    fun case_1_2(): Boolean? {
        contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(null) implies (<!UNRESOLVED_REFERENCE!>value_2<!>)<!> }
        return if (value_2) null else true
    }

    fun case_1_3(): Boolean {
        contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(true) implies (<!UNRESOLVED_REFERENCE!>value_3<!>)<!> }
        return value_3
    }
}

// TESTCASE NUMBER: 2
class case_2(value_5: Boolean, val value_1: Boolean) {
    val value_2 = getBoolean()

    companion object {
        const val value_3 = false
        private const val value_4 = true
    }

    init {
        fun case_2_1(): Boolean {
            contract { returns(false) implies (value_5) }
            return !(value_5)
        }
    }

    fun case_2_2(): Boolean? {
        contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(null) implies (<!UNRESOLVED_REFERENCE!>value_1<!>)<!> }
        return if (value_1) null else true
    }

    fun case_2_3(): Boolean {
        contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(true) implies (<!UNRESOLVED_REFERENCE!>value_2<!>)<!> }
        return value_2
    }

    fun case_2_4(): Boolean {
        contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(false) implies (<!UNRESOLVED_REFERENCE!>value_3<!>)<!> }
        return !(value_3)
    }

    inline fun <reified K : Number> K.case_2_5(): Boolean? {
        contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>returnsNotNull() implies (<!UNRESOLVED_REFERENCE!>value_4<!>)<!> }
        return if (<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>value_4<!>) true else null
    }
}
