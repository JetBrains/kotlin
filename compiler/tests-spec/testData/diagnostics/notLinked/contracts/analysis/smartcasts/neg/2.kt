// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !WITH_CONTRACT_FUNCTIONS
// SKIP_TXT

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)

 SECTION: contracts
 CATEGORY: analysis, smartcasts
 NUMBER: 2
 DESCRIPTION: Smartcasts using Returns effects with complex (conjunction/disjunction) type checking and not-null conditions outside contract (custom condition).
 */

fun case_1(value_1: Any?, value_2: Any?) {
    funWithReturns(value_1 !is String || value_2 !is Number)
    println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
    println(value_2.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
}

fun case_2(value_1: Any?, value_2: Any?) {
    funWithReturnsAndInvertCondition(value_1 is String && value_2 is Number)
    println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
    println(value_2.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
}

fun case_3(value_1: Any?, value_2: Any?) {
    funWithReturnsAndInvertCondition(value_1 is String && value_2 == null)
    println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
    println(value_2?.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
}

fun case_4(value_1: Any?, value_2: Number?) {
    funWithReturns(value_1 !is Float? || value_1 == null || value_2 == null)
    println(value_1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>dec<!>())
    println(value_2?.toByte())
}

class case_5_class {
    val prop_1: Int? = 10

    fun case_5(value_1: Any?, value_2: Number?) {
        val o = case_5_class()
        funWithReturns(value_1 !is Float? || value_1 == null || value_2 == null || o.prop_1 == null)
        println(value_1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>dec<!>())
        println(value_2?.toByte())
        println(o.prop_1<!UNSAFE_CALL!>.<!>plus(3))
    }
}

fun case_6(value_1: Any?, value_2: Any) {
    if (funWithReturnsTrue(value_1 !is String || value_2 !is Number)) {
        println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
        println(value_2.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
    }
    if (!funWithReturnsFalse(value_1 !is String || value_2 !is Number)) {
        println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
        println(value_2.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
    }
    if (funWithReturnsNotNull(value_1 !is String || value_2 !is Number) != null) {
        println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
        println(value_2.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
    }
    if (funWithReturnsNull(value_1 !is String || value_2 !is Number) == null) {
        println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
        println(value_2.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
    }
}

fun case_7(value_1: Any?, value_2: Any?) {
    if (funWithReturnsTrueAndInvertCondition(value_1 is String && value_2 is Number)) {
        println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
        println(value_2.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
    }
    if (!funWithReturnsFalseAndInvertCondition(value_1 is String && value_2 is Number)) {
        println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
        println(value_2.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
    }
    if (funWithReturnsNotNullAndInvertCondition(value_1 is String && value_2 is Number) != null) {
        println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
        println(value_2.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
    }
    if (funWithReturnsNullAndInvertCondition(value_1 is String && value_2 is Number) == null) {
        println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
        println(value_2.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
    }
}

fun case_8(value_1: Any?, value_2: Any?) {
    if (funWithReturnsTrueAndInvertCondition(value_1 is String && value_2 == null)) {
        println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
        println(value_2?.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
    }
    if (!funWithReturnsFalseAndInvertCondition(value_1 is String && value_2 == null)) {
        println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
        println(value_2?.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
    }
    if (funWithReturnsNotNullAndInvertCondition(value_1 is String && value_2 == null) != null) {
        println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
        println(value_2?.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
    }
    if (funWithReturnsNullAndInvertCondition(value_1 is String && value_2 == null) == null) {
        println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
        println(value_2?.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
    }
}

fun case_9(value_1: Any?, value_2: Number?) {
    if (funWithReturnsTrue(value_1 !is Float? || value_1 == null || value_2 == null)) {
        println(value_1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>dec<!>())
        println(value_2?.toByte())
    }
    if (!funWithReturnsFalse(value_1 !is Float? || value_1 == null || value_2 == null)) {
        println(value_1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>dec<!>())
        println(value_2?.toByte())
    }
    if (funWithReturnsNotNull(value_1 is Float? && value_1 != null && value_2 != null) == null) {
        println(value_1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>dec<!>())
        println(value_2?.toByte())
    }
    if (funWithReturnsNull(value_1 is Float? && value_1 != null && value_2 != null) != null) {
        println(value_1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>dec<!>())
        println(value_2?.toByte())
    }
}

class case_10_class {
    val prop_1: Int? = 10

    fun case_10(value_1: Any?, value_2: Number?) {
        val o = case_10_class()
        if (funWithReturnsTrue(value_1 !is Float? || value_1 == null || value_2 == null || o.prop_1 == null || this.prop_1 == null)) {
            println(value_1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>dec<!>())
            println(value_2?.toByte())
            println(o.prop_1<!UNSAFE_CALL!>.<!>plus(3))
        }
        if (!funWithReturnsFalse(value_1 !is Float? || value_1 == null || value_2 == null || o.prop_1 == null || this.prop_1 == null)) {
            println(value_1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>dec<!>())
            println(value_2?.toByte())
            println(o.prop_1<!UNSAFE_CALL!>.<!>plus(3))
        }
        if (funWithReturnsNotNull(value_1 !is Float? || value_1 == null || value_2 == null || o.prop_1 == null || this.prop_1 == null) != null) {
            println(value_1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>dec<!>())
            println(value_2?.toByte())
            println(o.prop_1<!UNSAFE_CALL!>.<!>plus(3))
        }
        if (funWithReturnsNull(value_1 !is Float? || value_1 == null || value_2 == null || o.prop_1 == null || this.prop_1 == null) == null) {
            println(value_1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>dec<!>())
            println(value_2?.toByte())
            println(o.prop_1<!UNSAFE_CALL!>.<!>plus(3))
        }
    }
}
