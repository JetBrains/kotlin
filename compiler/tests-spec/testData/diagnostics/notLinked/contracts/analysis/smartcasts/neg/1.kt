// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !WITH_CONTRACT_FUNCTIONS
// SKIP_TXT

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)

 SECTION: contracts
 CATEGORY: analysis, smartcasts
 NUMBER: 1
 DESCRIPTION: Smartcasts using Returns effects with simple type checking, not-null conditions and custom condition (condition for smartcast outside contract).
 */

fun case_1(value_1: Any?) {
    funWithReturns(value_1 !is String)
    println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
}

fun case_2(value_1: Int?) {
    funWithReturnsAndInvertCondition(value_1 != null)
    println(<!DEBUG_INFO_CONSTANT!>value_1<!><!UNSAFE_CALL!>.<!>inc())
}

fun case_3(value_1: Int?) {
    funWithReturns(value_1 == null)
    println(<!DEBUG_INFO_CONSTANT!>value_1<!><!UNSAFE_CALL!>.<!>inc())
}

fun case_4(value_1: Any?) {
    funWithReturnsAndInvertTypeCheck(value_1)
    println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
}

fun case_5(value_1: String?) {
    funWithReturnsAndNullCheck(value_1)
    println(<!DEBUG_INFO_CONSTANT!>value_1<!><!UNSAFE_CALL!>.<!>length)
}

fun case_6(value_1: String?) {
    funWithReturnsAndNullCheck(value_1)
    println(<!DEBUG_INFO_CONSTANT!>value_1<!><!UNSAFE_CALL!>.<!>length)
}

object case_7_object {
    val prop_1: Int? = 10
}
fun case_7() {
    funWithReturns(case_7_object.prop_1 == null)
    <!DEBUG_INFO_CONSTANT!>case_7_object.prop_1<!><!UNSAFE_CALL!>.<!>inc()
}

fun case_8(value_1: Any?) {
    if (!funWithReturnsTrue(value_1 is String)) println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
    if (!funWithReturnsTrueAndInvertCondition(value_1 !is String)) println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
    if (funWithReturnsFalse(value_1 is String)) println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
    if (funWithReturnsFalseAndInvertCondition(value_1 !is String)) println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
    if (funWithReturnsNotNull(value_1 is String) == null) println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
    if (!(funWithReturnsNotNull(value_1 is String) != null)) println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
    if (!(funWithReturnsNull(value_1 is String) == null)) println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
    if (funWithReturnsNull(value_1 is String) != null) println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
}

fun case_9(value_1: String?) {
    if (!funWithReturnsTrue(value_1 != null)) println(value_1<!UNSAFE_CALL!>.<!>length)
    if (!funWithReturnsTrueAndInvertCondition(value_1 == null)) println(value_1<!UNSAFE_CALL!>.<!>length)
    if (funWithReturnsFalse(value_1 != null)) println(value_1<!UNSAFE_CALL!>.<!>length)
    if (funWithReturnsFalseAndInvertCondition(value_1 == null)) println(value_1<!UNSAFE_CALL!>.<!>length)
    if (funWithReturnsNotNull(value_1 != null) == null) println(value_1<!UNSAFE_CALL!>.<!>length)
    if (funWithReturnsNotNullAndInvertCondition(value_1 == null) == null) println(value_1<!UNSAFE_CALL!>.<!>length)
    if (funWithReturnsNull(value_1 != null) != null) println(value_1<!UNSAFE_CALL!>.<!>length)
    if (funWithReturnsNullAndInvertCondition(value_1 == null) != null) println(value_1<!UNSAFE_CALL!>.<!>length)
}

fun case_10(value_1: Any?) {
    if (!funWithReturnsTrueAndTypeCheck(value_1)) println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
    if (!!funWithReturnsFalseAndTypeCheck(value_1)) println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
    if (!(funWithReturnsNotNullAndTypeCheck(value_1) != null)) println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
    if (!!(funWithReturnsNotNullAndTypeCheck(value_1) == null)) println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
    if (!!(funWithReturnsNullAndTypeCheck(value_1) != null)) println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
    if (!(funWithReturnsNullAndTypeCheck(value_1) == null)) println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
}

fun case_11(value_1: Number?) {
    if (!funWithReturnsTrueAndNotNullCheck(value_1)) println(value_1<!UNSAFE_CALL!>.<!>toByte())
    if (!funWithReturnsTrueAndNullCheck(value_1)) println(value_1)
    if (funWithReturnsFalseAndNotNullCheck(value_1)) println(value_1<!UNSAFE_CALL!>.<!>toByte())
    if (funWithReturnsFalseAndNullCheck(value_1)) println(value_1)
    if ((funWithReturnsNotNullAndNotNullCheck(value_1) == null)) println(value_1<!UNSAFE_CALL!>.<!>toByte())
    if (!!!(funWithReturnsNotNullAndNotNullCheck(value_1) != null)) println(value_1<!UNSAFE_CALL!>.<!>toByte())
    if (!!(funWithReturnsNotNullAndNullCheck(value_1) == null)) println(value_1)
    if (!(funWithReturnsNullAndNotNullCheck(value_1) == null)) println(value_1<!UNSAFE_CALL!>.<!>toByte())
    if (!!(funWithReturnsNullAndNotNullCheck(value_1) != null)) println(value_1<!UNSAFE_CALL!>.<!>toByte())
    if (!!!(funWithReturnsNullAndNullCheck(value_1) == null)) println(value_1)
}
