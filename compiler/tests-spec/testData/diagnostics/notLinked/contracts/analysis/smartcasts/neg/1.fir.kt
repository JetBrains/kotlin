// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !WITH_NEW_INFERENCE
// SKIP_TXT

// TESTCASE NUMBER: 1
fun case_1(value_1: Any?) {
    funWithReturns(value_1 !is String)
    <!AMBIGUITY!>println<!>(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
}

// TESTCASE NUMBER: 2
fun case_2(value_1: Int?) {
    funWithReturnsAndInvertCondition(value_1 != null)
    <!AMBIGUITY!>println<!>(value_1.<!AMBIGUITY!>inc<!>())
}

// TESTCASE NUMBER: 3
fun case_3(value_1: Int?) {
    funWithReturns(value_1 == null)
    <!AMBIGUITY!>println<!>(value_1.<!AMBIGUITY!>inc<!>())
}

// TESTCASE NUMBER: 4
fun case_4(value_1: Any?) {
    funWithReturnsAndInvertTypeCheck(value_1)
    <!AMBIGUITY!>println<!>(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
}

// TESTCASE NUMBER: 5
fun case_5(value_1: String?) {
    funWithReturnsAndNullCheck(value_1)
    println(value_1.<!INAPPLICABLE_CANDIDATE!>length<!>)
}

// TESTCASE NUMBER: 6
fun case_6(value_1: String?) {
    funWithReturnsAndNullCheck(value_1)
    println(value_1.<!INAPPLICABLE_CANDIDATE!>length<!>)
}

// TESTCASE NUMBER: 7
object case_7_object {
    val prop_1: Int? = 10
}
fun case_7() {
    funWithReturns(case_7_object.prop_1 == null)
    case_7_object.prop_1.<!AMBIGUITY!>inc<!>()
}

// TESTCASE NUMBER: 8
fun case_8(value_1: Any?) {
    if (!funWithReturnsTrue(value_1 is String)) <!AMBIGUITY!>println<!>(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
    if (!funWithReturnsTrueAndInvertCondition(value_1 !is String)) <!AMBIGUITY!>println<!>(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
    if (funWithReturnsFalse(value_1 is String)) <!AMBIGUITY!>println<!>(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
    if (funWithReturnsFalseAndInvertCondition(value_1 !is String)) <!AMBIGUITY!>println<!>(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
    if (funWithReturnsNotNull(value_1 is String) == null) <!AMBIGUITY!>println<!>(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
    if (!(funWithReturnsNotNull(value_1 is String) != null)) <!AMBIGUITY!>println<!>(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
    if (!(funWithReturnsNull(value_1 is String) == null)) <!AMBIGUITY!>println<!>(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
    if (funWithReturnsNull(value_1 is String) != null) <!AMBIGUITY!>println<!>(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
}

// TESTCASE NUMBER: 9
fun case_9(value_1: String?) {
    if (!funWithReturnsTrue(value_1 != null)) println(value_1.<!INAPPLICABLE_CANDIDATE!>length<!>)
    if (!funWithReturnsTrueAndInvertCondition(value_1 == null)) println(value_1.<!INAPPLICABLE_CANDIDATE!>length<!>)
    if (funWithReturnsFalse(value_1 != null)) println(value_1.<!INAPPLICABLE_CANDIDATE!>length<!>)
    if (funWithReturnsFalseAndInvertCondition(value_1 == null)) println(value_1.<!INAPPLICABLE_CANDIDATE!>length<!>)
    if (funWithReturnsNotNull(value_1 != null) == null) println(value_1.<!INAPPLICABLE_CANDIDATE!>length<!>)
    if (funWithReturnsNotNullAndInvertCondition(value_1 == null) == null) println(value_1.<!INAPPLICABLE_CANDIDATE!>length<!>)
    if (funWithReturnsNull(value_1 != null) != null) println(value_1.<!INAPPLICABLE_CANDIDATE!>length<!>)
    if (funWithReturnsNullAndInvertCondition(value_1 == null) != null) println(value_1.<!INAPPLICABLE_CANDIDATE!>length<!>)
}

// TESTCASE NUMBER: 10
fun case_10(value_1: Any?) {
    if (!funWithReturnsTrueAndTypeCheck(value_1)) <!AMBIGUITY!>println<!>(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
    if (!!funWithReturnsFalseAndTypeCheck(value_1)) <!AMBIGUITY!>println<!>(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
    if (!(funWithReturnsNotNullAndTypeCheck(value_1) != null)) <!AMBIGUITY!>println<!>(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
    if (!!(funWithReturnsNotNullAndTypeCheck(value_1) == null)) <!AMBIGUITY!>println<!>(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
    if (!!(funWithReturnsNullAndTypeCheck(value_1) != null)) <!AMBIGUITY!>println<!>(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
    if (!(funWithReturnsNullAndTypeCheck(value_1) == null)) <!AMBIGUITY!>println<!>(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
}

// TESTCASE NUMBER: 11
fun case_11(value_1: Number?) {
    if (!funWithReturnsTrueAndNotNullCheck(value_1)) println(value_1.<!INAPPLICABLE_CANDIDATE!>toByte<!>())
    if (!funWithReturnsTrueAndNullCheck(value_1)) println(value_1)
    if (funWithReturnsFalseAndNotNullCheck(value_1)) println(value_1.<!INAPPLICABLE_CANDIDATE!>toByte<!>())
    if (funWithReturnsFalseAndNullCheck(value_1)) println(value_1)
    if ((funWithReturnsNotNullAndNotNullCheck(value_1) == null)) println(value_1.<!INAPPLICABLE_CANDIDATE!>toByte<!>())
    if (!!!(funWithReturnsNotNullAndNotNullCheck(value_1) != null)) println(value_1.<!INAPPLICABLE_CANDIDATE!>toByte<!>())
    if (!!(funWithReturnsNotNullAndNullCheck(value_1) == null)) println(value_1)
    if (!(funWithReturnsNullAndNotNullCheck(value_1) == null)) println(value_1.<!INAPPLICABLE_CANDIDATE!>toByte<!>())
    if (!!(funWithReturnsNullAndNotNullCheck(value_1) != null)) println(value_1.<!INAPPLICABLE_CANDIDATE!>toByte<!>())
    if (!!!(funWithReturnsNullAndNullCheck(value_1) == null)) println(value_1)
}
