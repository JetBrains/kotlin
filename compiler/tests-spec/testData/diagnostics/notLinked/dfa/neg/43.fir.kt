// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_VALUE -VARIABLE_WITH_REDUNDANT_INITIALIZER
// SKIP_TXT

/*
 * TESTCASE NUMBER: 1
 * ISSUES: KT-10461
 */
fun case_1(x: Double?, y: Double?) : Double {
    return if (x == null && y == null) {
        0.0
    } else if (x != null && y == null) {
        x
    } else if (x == null && y != null) {
        y
    } else {
        x <!NONE_APPLICABLE!>+<!> y
    }
}

/*
 * TESTCASE NUMBER: 2
 * ISSUES: KT-8781
 */
fun case_2(x: Boolean?, y: Any?) {
    if (x == true) return
    if (x == false) return
    if (y != x) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>y<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>y<!><!UNSAFE_CALL!>.<!>equals(10)
    }
}

/*
 * TESTCASE NUMBER: 3
 * ISSUES: KT-8781
 */
fun case_3(x : Unit?, y : Any?) {
    if (x == kotlin.Unit) return
    if (y != x) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>y<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>y<!><!UNSAFE_CALL!>.<!>equals(10)
    }
}

/*
 * TESTCASE NUMBER: 4
 * ISSUES: KT-8781
 */
fun case_4(x : EnumClassSingle?, y : Any?) {
    if (x == EnumClassSingle.EVERYTHING) return
    if (y != x) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>y<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>y<!><!UNSAFE_CALL!>.<!>equals(10)
    }
}

/*
 * TESTCASE NUMBER: 5
 * ISSUES: KT-18950
 */
fun case_5(x: SealedClass) {
    when (x) {
        is SealedChild1 -> x.number
        is SealedChild2 -> x.e1 + x.e2
        else -> x.<!UNRESOLVED_REFERENCE!>m1<!> + x.<!UNRESOLVED_REFERENCE!>m1<!>
    }
}

/*
 * TESTCASE NUMBER: 6
 * ISSUES: KT-18950
 */
fun case_6(x: SealedClass?) {
    when (x) {
        is SealedChild1 -> x.number
        is SealedChild2 -> x.e1 + x.e2
        null -> {}
        else -> x.<!UNRESOLVED_REFERENCE!>m1<!> + x.<!UNRESOLVED_REFERENCE!>m1<!>
    }
}
