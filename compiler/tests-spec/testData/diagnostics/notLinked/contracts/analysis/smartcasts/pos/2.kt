// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !WITH_CONTRACT_FUNCTIONS
// SKIP_TXT

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)

 SECTION: contracts
 CATEGORY: analysis, smartcasts
 NUMBER: 2
 DESCRIPTION: Smartcasts using Returns effects with complex (conjunction/disjunction) type checking and not-null conditions outside contract (custom condition).
 */

fun case_1(value_1: Any?, value_2: Any?) {
    funWithReturns(value_1 is String && value_2 is Number)
    println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
    println(<!DEBUG_INFO_SMARTCAST!>value_2<!>.toByte())
}

fun case_2(value_1: Any?, value_2: Any?) {
    funWithReturnsAndInvertCondition(value_1 !is String || value_2 !is Number)
    println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
    println(<!DEBUG_INFO_SMARTCAST!>value_2<!>.toByte())
}

fun case_3(value_1: Any?, value_2: Any?) {
    funWithReturnsAndInvertCondition(value_1 !is String || value_2 != null)
    println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
    println(<!DEBUG_INFO_CONSTANT!>value_2<!>?.toByte())
}

fun case_4(value_1: Any?, value_2: Number?) {
    funWithReturns(value_1 is Float? && value_1 != null && value_2 != null)
    println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.dec())
    println(value_2<!UNNECESSARY_SAFE_CALL!>?.<!>toByte())
}

class case_5_class {
    val prop_1: Int? = 10

    fun case_5(value_1: Any?, value_2: Number?) {
        val o = case_5_class()
        funWithReturns(value_1 is Float? && value_1 != null && value_2 != null && o.prop_1 != null)
        println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.dec())
        println(value_2<!UNNECESSARY_SAFE_CALL!>?.<!>toByte())
        println(<!DEBUG_INFO_SMARTCAST!>o.prop_1<!>.plus(3))
    }
}

fun case_6(value_1: Any?, value_2: Any) {
    if (funWithReturnsTrue(value_1 is String && value_2 is Number)) {
        println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
        println(<!DEBUG_INFO_SMARTCAST!>value_2<!>.toByte())
    }
    if (!funWithReturnsFalse(value_1 is String && value_2 is Number)) {
        println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
        println(<!DEBUG_INFO_SMARTCAST!>value_2<!>.toByte())
    }
    if (funWithReturnsNotNull(value_1 is String && value_2 is Number) != null) {
        println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
        println(<!DEBUG_INFO_SMARTCAST!>value_2<!>.toByte())
    }
    if (funWithReturnsNull(value_1 is String && value_2 is Number) == null) {
        println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
        println(<!DEBUG_INFO_SMARTCAST!>value_2<!>.toByte())
    }
}

fun case_7(value_1: Any?, value_2: Any?) {
    if (funWithReturnsTrueAndInvertCondition(value_1 !is String || value_2 !is Number)) {
        println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
        println(<!DEBUG_INFO_SMARTCAST!>value_2<!>.toByte())
    }
    if (!funWithReturnsFalseAndInvertCondition(value_1 !is String || value_2 !is Number)) {
        println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
        println(<!DEBUG_INFO_SMARTCAST!>value_2<!>.toByte())
    }
    if (funWithReturnsNotNullAndInvertCondition(value_1 !is String || value_2 !is Number) != null) {
        println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
        println(<!DEBUG_INFO_SMARTCAST!>value_2<!>.toByte())
    }
    if (funWithReturnsNullAndInvertCondition(value_1 !is String || value_2 !is Number) == null) {
        println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
        println(<!DEBUG_INFO_SMARTCAST!>value_2<!>.toByte())
    }
}

fun case_8(value_1: Any?, value_2: Any?) {
    if (funWithReturnsTrueAndInvertCondition(value_1 !is String || value_2 != null)) {
        println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
        println(<!DEBUG_INFO_CONSTANT!>value_2<!>?.toByte())
    }
    if (!funWithReturnsFalseAndInvertCondition(value_1 !is String || value_2 != null)) {
        println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
        println(<!DEBUG_INFO_CONSTANT!>value_2<!>?.toByte())
    }
    if (funWithReturnsNotNullAndInvertCondition(value_1 !is String || value_2 != null) != null) {
        println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
        println(<!DEBUG_INFO_CONSTANT!>value_2<!>?.toByte())
    }
    if (funWithReturnsNullAndInvertCondition(value_1 !is String || value_2 != null) == null) {
        println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
        println(<!DEBUG_INFO_CONSTANT!>value_2<!>?.toByte())
    }
}

fun case_9(value_1: Any?, value_2: Number?) {
    if (funWithReturnsTrue(value_1 is Float? && value_1 != null && value_2 != null)) {
        println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.dec())
        println(value_2<!UNNECESSARY_SAFE_CALL!>?.<!>toByte())
    }
    if (!funWithReturnsFalse(value_1 is Float? && value_1 != null && value_2 != null)) {
        println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.dec())
        println(value_2<!UNNECESSARY_SAFE_CALL!>?.<!>toByte())
    }
    if (funWithReturnsNotNull(value_1 is Float? && value_1 != null && value_2 != null) != null) {
        println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.dec())
        println(value_2<!UNNECESSARY_SAFE_CALL!>?.<!>toByte())
    }
    if (funWithReturnsNull(value_1 is Float? && value_1 != null && value_2 != null) == null) {
        println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.dec())
        println(value_2<!UNNECESSARY_SAFE_CALL!>?.<!>toByte())
    }
}

class case_10_class {
    val prop_1: Int? = 10

    fun case_10(value_1: Any?, value_2: Number?) {
        val o = case_10_class()
        if (funWithReturnsTrue(value_1 is Float? && value_1 != null && value_2 != null && o.prop_1 != null && this.prop_1 != null)) {
            println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.dec())
            println(value_2<!UNNECESSARY_SAFE_CALL!>?.<!>toByte())
            println(<!DEBUG_INFO_SMARTCAST!>o.prop_1<!>.plus(3))
        }
        if (!funWithReturnsFalse(value_1 is Float? && value_1 != null && value_2 != null && o.prop_1 != null && this.prop_1 != null)) {
            println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.dec())
            println(value_2<!UNNECESSARY_SAFE_CALL!>?.<!>toByte())
            println(<!DEBUG_INFO_SMARTCAST!>o.prop_1<!>.plus(3))
        }
        if (funWithReturnsNotNull(value_1 is Float? && value_1 != null && value_2 != null && o.prop_1 != null && this.prop_1 != null) != null) {
            println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.dec())
            println(value_2<!UNNECESSARY_SAFE_CALL!>?.<!>toByte())
            println(<!DEBUG_INFO_SMARTCAST!>o.prop_1<!>.plus(3))
        }
        if (funWithReturnsNull(value_1 is Float? && value_1 != null && value_2 != null && o.prop_1 != null && this.prop_1 != null) == null) {
            println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.dec())
            println(value_2<!UNNECESSARY_SAFE_CALL!>?.<!>toByte())
            println(<!DEBUG_INFO_SMARTCAST!>o.prop_1<!>.plus(3))
        }
    }
}
