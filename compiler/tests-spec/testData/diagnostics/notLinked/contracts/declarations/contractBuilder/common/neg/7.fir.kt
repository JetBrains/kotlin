// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -UNREACHABLE_CODE -UNUSED_EXPRESSION
// !OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

// TESTCASE NUMBER: 1
fun <T> T?.case_1() {
    fun <K> K?.case_1_1(): Boolean {
        contract { returns(true) implies (this@case_1 != null) }
        return this@case_1 != null
    }
}
