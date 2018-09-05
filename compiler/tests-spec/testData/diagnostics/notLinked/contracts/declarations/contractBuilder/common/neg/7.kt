// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER -UNUSED_VARIABLE -UNUSED_PARAMETER -UNREACHABLE_CODE -UNUSED_EXPRESSION
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !WITH_BASIC_TYPES

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)

 SECTION: contracts
 CATEGORY: declarations, contractBuilder, common
 NUMBER: 7
 DESCRIPTION: Contract function with 'this' labeled by not current extensible object
 ISSUES: KT-26149
 */

import kotlin.contracts.*

fun <T> T?.case_3() {
    fun <K> K?.case_3_1(): Boolean {
        <!CONTRACT_NOT_ALLOWED!>contract<!> { returns(true) implies (this@case_3 != null) }
        return this@case_3 != null
    }
}
