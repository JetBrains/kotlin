// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !WITH_CONTRACT_FUNCTIONS
// SKIP_TXT

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)

 SECTION: contracts
 CATEGORY: analysis, smartcasts
 NUMBER: 1
 DESCRIPTION: Smartcasts using Returns effects with simple type checking, not-null conditions and custom condition (condition for smartcast outside contract).
 */

fun case_1(value_1: Any?) {
    funWithReturns(value_1 is String)
    println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
}

fun case_2(value_1: Int?) {
    funWithReturns(value_1 != null)
    println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.inc())
}

fun case_3(value_1: Int?) {
    funWithReturns(value_1 == null)
    println(<!DEBUG_INFO_CONSTANT!>value_1<!>)
}

fun case_4(value_1: Any?) {
    funWithReturnsAndTypeCheck(value_1)
    println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
}

fun case_5(value_1: String?) {
    funWithReturnsAndNotNullCheck(value_1)
    println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
}

fun case_6(value_1: String?) {
    funWithReturnsAndNullCheck(value_1)
    println(<!DEBUG_INFO_CONSTANT!>value_1<!>)
}

object case_7_object {
    val prop_1: Int? = 10
}
fun case_7() {
    funWithReturnsAndInvertCondition(case_7_object.prop_1 == null)
    <!DEBUG_INFO_SMARTCAST!>case_7_object.prop_1<!>.inc()
}

fun case_8(value_1: Any?) {
    if (funWithReturnsTrue(value_1 is String)) println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
    if (funWithReturnsTrueAndInvertCondition(value_1 !is String)) println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
    if (!funWithReturnsFalse(value_1 is String)) println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
    if (!funWithReturnsFalseAndInvertCondition(value_1 !is String)) println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
    if (funWithReturnsNotNull(value_1 is String) != null) println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
    if (!(funWithReturnsNotNull(value_1 is String) == null)) println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
}

fun case_9(value_1: String?) {
    if (funWithReturnsTrue(value_1 != null)) println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
    if (funWithReturnsTrueAndInvertCondition(value_1 == null)) println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
    if (!funWithReturnsFalse(value_1 != null)) println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
    if (!funWithReturnsFalseAndInvertCondition(value_1 == null)) println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
    if (funWithReturnsNotNull(value_1 != null) != null) println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
    if (!(funWithReturnsNotNull(value_1 != null) == null)) println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
    if (!(funWithReturnsNotNullAndInvertCondition(value_1 == null) == null)) println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
}

fun case_10(value_1: Any?) {
    if (funWithReturnsTrueAndTypeCheck(value_1)) println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
    if (!funWithReturnsFalseAndTypeCheck(value_1)) println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
    if (funWithReturnsNotNullAndTypeCheck(value_1) != null) println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
    if (!(funWithReturnsNotNullAndTypeCheck(value_1) == null)) println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
}

fun case_11(value_1: Number?, value_2: Int?) {
    if (funWithReturnsTrueAndNotNullCheck(value_1)) println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.toByte())
    if (funWithReturnsTrueAndNullCheck(value_1)) println(<!DEBUG_INFO_CONSTANT!>value_1<!>)
    if (!funWithReturnsFalseAndNotNullCheck(value_2)) <!DEBUG_INFO_SMARTCAST!>value_2<!>.inc()
    if (!funWithReturnsFalseAndNotNullCheck(value_1)) println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.toByte())
    if (!funWithReturnsFalseAndNullCheck(value_1)) println(<!DEBUG_INFO_CONSTANT!>value_1<!>)
    if (!(funWithReturnsNotNullAndNotNullCheck(value_1) == null)) println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.toByte())
    if (funWithReturnsNotNullAndNotNullCheck(value_1) != null) println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.toByte())
    if (funWithReturnsNotNullAndNullCheck(value_1) != null) println(<!DEBUG_INFO_CONSTANT!>value_1<!>)
}
