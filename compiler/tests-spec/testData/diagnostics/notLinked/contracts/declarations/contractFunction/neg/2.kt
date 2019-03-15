// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: contracts, declarations, contractFunction
 * NUMBER: 2
 * DESCRIPTION: Check report about use contracts in literal functions, lambdas or not top-level functions.
 * ISSUES: KT-26149
 * HELPERS: classes
 */

import kotlin.contracts.*

// TESTCASE NUMBER: 1
fun case_1() {
    val fun_1 = fun(block: () -> Unit) {
        <!CONTRACT_NOT_ALLOWED!>contract<!> { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        return block()
    }

    fun_1 { throw Exception() }
    println("1")
}

// TESTCASE NUMBER: 2
fun case_2() {
    val lambda_1 = { block: () -> Unit ->
        <!CONTRACT_NOT_ALLOWED!>contract<!> { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        block()
    }

    lambda_1 { throw Exception() }
    println("1")
}

// TESTCASE NUMBER: 3
object case_3 {
    fun case_3(block: () -> Unit) {
        <!CONTRACT_NOT_ALLOWED!>contract<!> { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        return block()
    }
}

/*
 * TESTCASE NUMBER: 4
 * ISSUES: KT-26244
 */
class case_4 : ClassLevel3() {

    fun <T : Number?>T.case_4_1(): Boolean {
        <!CONTRACT_NOT_ALLOWED!>contract<!> { returns(false) implies (<!USELESS_IS_CHECK!>this@case_4 !is ClassLevel1<!>) }
        return this == null
    }

    fun case_4_2(number: Int?): Boolean {
        <!CONTRACT_NOT_ALLOWED!>contract<!> { returns(false) implies (number != null) }
        return number == null
    }

    fun <T>T?.case_4_3(): Boolean {
        <!CONTRACT_NOT_ALLOWED!>contract<!> { returns(false) implies (this@case_4_3 !is Number) }
        return this@case_4_3 is Number
    }

    fun <T : <!FINAL_UPPER_BOUND!>Boolean<!>>T.case_4_4() {
        <!CONTRACT_NOT_ALLOWED!>contract<!> { returns() implies (!this@case_4_4) }
        if (this) throw Exception()
    }

    fun <T>T.case_4_5_wrap() {
        fun case_4_5_contract() {
            <!CONTRACT_NOT_ALLOWED!>contract<!> { returns() implies (this@case_4_5_wrap is ClassLevel1) }
            if (this@case_4_5_wrap !is ClassLevel1) throw Exception()
        }
        case_4_5_contract()
        println("!")
    }

    fun case_4_5() = ClassLevel3().case_4_5_wrap()
}

/*
 * TESTCASE NUMBER: 5
 * ISSUES: KT-26244
 */
class case_5<T> : ClassLevel5() {
    inner class case_5_1 {
        fun <K : Number?>K.case_5_1_1() {
            <!CONTRACT_NOT_ALLOWED!>contract<!> { returns() implies (this@case_5_1 !is ClassLevel1 && <!SENSELESS_COMPARISON!>this@case_5_1 != null<!> || <!USELESS_IS_CHECK!>this@case_5 is ClassLevel1<!> && this@case_5_1_1 is Float) }
            if (!(this@case_5_1 !is ClassLevel1 && <!SENSELESS_COMPARISON!>this@case_5_1 != null<!> || <!USELESS_IS_CHECK!>this@case_5 is ClassLevel1<!> && this is Float)) throw Exception()
        }

        fun case_5_1_2() {
            <!CONTRACT_NOT_ALLOWED!>contract<!> { returns() implies (this@case_5_1 !is ClassLevel1 || <!USELESS_IS_CHECK!>this@case_5 is ClassLevel1<!> || <!SENSELESS_COMPARISON!>this@case_5_1 == null<!>) }
            if (!(this@case_5_1 !is ClassLevel1 || <!USELESS_IS_CHECK!>this@case_5 is ClassLevel1<!> || <!SENSELESS_COMPARISON!>this@case_5_1 == null<!>)) throw Exception()
        }
    }
}
