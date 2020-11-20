// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !WITH_NEW_INFERENCE
// SKIP_TXT

// TESTCASE NUMBER: 1
fun case_1(value_1: Any?, value_2: Any?) {
    funWithReturns(value_1 is String && value_2 is Number)
    println(value_1.length)
    println(value_2.toByte())
}

// TESTCASE NUMBER: 2
fun case_2(value_1: Any?, value_2: Any?) {
    funWithReturnsAndInvertCondition(value_1 !is String || value_2 !is Number)
    println(value_1.length)
    println(value_2.toByte())
}

// TESTCASE NUMBER: 3
fun case_3(value_1: Any?, value_2: Any?) {
    funWithReturnsAndInvertCondition(value_1 !is String || value_2 != null)
    println(value_1.length)
    <!AMBIGUITY!>println<!>(value_2?.<!UNRESOLVED_REFERENCE!>toByte<!>())
}

// TESTCASE NUMBER: 4
fun case_4(value_1: Any?, value_2: Number?) {
    funWithReturns(value_1 is Float? && value_1 != null && value_2 != null)
    println(value_1.dec())
    println(value_2?.toByte())
}

// TESTCASE NUMBER: 5
class case_5_class {
    val prop_1: Int? = 10

    fun case_5(value_1: Any?, value_2: Number?) {
        val o = case_5_class()
        funWithReturns(value_1 is Float? && value_1 != null && value_2 != null && o.prop_1 != null)
        println(value_1.dec())
        println(value_2?.toByte())
        println(o.prop_1.plus(3))
    }
}

// TESTCASE NUMBER: 6
fun case_6(value_1: Any?, value_2: Any) {
    if (funWithReturnsTrue(value_1 is String && value_2 is Number)) {
        println(value_1.length)
        println(value_2.toByte())
    }
    if (!funWithReturnsFalse(value_1 is String && value_2 is Number)) {
        println(value_1.length)
        println(value_2.toByte())
    }
    if (funWithReturnsNotNull(value_1 is String && value_2 is Number) != null) {
        println(value_1.length)
        println(value_2.toByte())
    }
    if (funWithReturnsNull(value_1 is String && value_2 is Number) == null) {
        println(value_1.length)
        println(value_2.toByte())
    }
}

// TESTCASE NUMBER: 7
fun case_7(value_1: Any?, value_2: Any?) {
    if (funWithReturnsTrueAndInvertCondition(value_1 !is String || value_2 !is Number)) {
        println(value_1.length)
        println(value_2.toByte())
    }
    if (!funWithReturnsFalseAndInvertCondition(value_1 !is String || value_2 !is Number)) {
        println(value_1.length)
        println(value_2.toByte())
    }
    if (funWithReturnsNotNullAndInvertCondition(value_1 !is String || value_2 !is Number) != null) {
        println(value_1.length)
        println(value_2.toByte())
    }
    if (funWithReturnsNullAndInvertCondition(value_1 !is String || value_2 !is Number) == null) {
        println(value_1.length)
        println(value_2.toByte())
    }
}

// TESTCASE NUMBER: 8
fun case_8(value_1: Any?, value_2: Any?) {
    if (funWithReturnsTrueAndInvertCondition(value_1 !is String || value_2 != null)) {
        println(value_1.length)
        <!AMBIGUITY!>println<!>(value_2?.<!UNRESOLVED_REFERENCE!>toByte<!>())
    }
    if (!funWithReturnsFalseAndInvertCondition(value_1 !is String || value_2 != null)) {
        println(value_1.length)
        <!AMBIGUITY!>println<!>(value_2?.<!UNRESOLVED_REFERENCE!>toByte<!>())
    }
    if (funWithReturnsNotNullAndInvertCondition(value_1 !is String || value_2 != null) != null) {
        println(value_1.length)
        <!AMBIGUITY!>println<!>(value_2?.<!UNRESOLVED_REFERENCE!>toByte<!>())
    }
    if (funWithReturnsNullAndInvertCondition(value_1 !is String || value_2 != null) == null) {
        println(value_1.length)
        <!AMBIGUITY!>println<!>(value_2?.<!UNRESOLVED_REFERENCE!>toByte<!>())
    }
}

// TESTCASE NUMBER: 9
fun case_9(value_1: Any?, value_2: Number?) {
    if (funWithReturnsTrue(value_1 is Float? && value_1 != null && value_2 != null)) {
        println(value_1.dec())
        println(value_2?.toByte())
    }
    if (!funWithReturnsFalse(value_1 is Float? && value_1 != null && value_2 != null)) {
        println(value_1.dec())
        println(value_2?.toByte())
    }
    if (funWithReturnsNotNull(value_1 is Float? && value_1 != null && value_2 != null) != null) {
        println(value_1.dec())
        println(value_2?.toByte())
    }
    if (funWithReturnsNull(value_1 is Float? && value_1 != null && value_2 != null) == null) {
        println(value_1.dec())
        println(value_2?.toByte())
    }
}

// TESTCASE NUMBER: 10
class case_10_class {
    val prop_1: Int? = 10

    fun case_10(value_1: Any?, value_2: Number?) {
        val o = case_10_class()
        if (funWithReturnsTrue(value_1 is Float? && value_1 != null && value_2 != null && o.prop_1 != null && this.prop_1 != null)) {
            println(value_1.dec())
            println(value_2?.toByte())
            println(o.prop_1.plus(3))
        }
        if (!funWithReturnsFalse(value_1 is Float? && value_1 != null && value_2 != null && o.prop_1 != null && this.prop_1 != null)) {
            println(value_1.dec())
            println(value_2?.toByte())
            println(o.prop_1.plus(3))
        }
        if (funWithReturnsNotNull(value_1 is Float? && value_1 != null && value_2 != null && o.prop_1 != null && this.prop_1 != null) != null) {
            println(value_1.dec())
            println(value_2?.toByte())
            println(o.prop_1.plus(3))
        }
        if (funWithReturnsNull(value_1 is Float? && value_1 != null && value_2 != null && o.prop_1 != null && this.prop_1 != null) == null) {
            println(value_1.dec())
            println(value_2?.toByte())
            println(o.prop_1.plus(3))
        }
    }
}

/*
 * TESTCASE NUMBER: 11
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-26747
 */
fun case_11(value_1: Any?, value_2: Any?, value_3: Any?) {
    funWithReturnsAndInvertCondition(value_1 !is String || value_2 !is Number || value_3 !is Any?)
    println(value_1!!.length)
    println(value_2?.toByte())
}
