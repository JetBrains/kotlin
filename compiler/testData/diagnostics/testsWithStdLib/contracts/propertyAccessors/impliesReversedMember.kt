// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +AllowContractsOnPropertyAccessors
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts

import kotlin.contracts.*
class Host {
    val String?.fooM: String?
        get() {
            contract { (this@fooM == null) implies returnsNotNull() }
            return if (this == null) "" else null
        }

    fun usageMember1() {
        "".fooM<!UNSAFE_CALL!>.<!>length
    }

    fun usageMember2() {
        val x: String? = null
        x.fooM<!UNSAFE_CALL!>.<!>length
    }

    val Any.barM: String?
        get() {
            contract { (this@barM is String) implies returnsNotNull() }
            return if (this is String) "" else null
        }

    fun usageMember3() {
        val s: Any = ""
        s.barM<!UNSAFE_CALL!>.<!>length
    }

    fun usageMember4() {
        val n: Any = 1
        n.barM<!UNSAFE_CALL!>.<!>length
    }
}

fun fooTop(x: String?): String? {
    contract { (x == null) implies returnsNotNull() }
    return if (x == null) "" else null
}

fun usageTop1() {
    fooTop("")<!UNSAFE_CALL!>.<!>length
}

fun usageTop2() {
    val x: String? = null
    fooTop(x)<!UNSAFE_CALL!>.<!>length
}

fun barTop(x: Any): String? {
    contract { (x is String) implies returnsNotNull() }
    return if (x is String) "" else null
}

fun usageTop3() {
    val s: Any = ""
    barTop(s)<!UNSAFE_CALL!>.<!>length
}

fun usageTop4() {
    val n: Any = 1
    barTop(n)<!UNSAFE_CALL!>.<!>length
}

/* GENERATED_FIR_TAGS: classDeclaration, contractImpliesReturnEffect, contracts, equalityExpression, functionDeclaration,
getter, ifExpression, integerLiteral, isExpression, lambdaLiteral, localProperty, nullableType, propertyDeclaration,
propertyWithExtensionReceiver, smartcast, stringLiteral, thisExpression */
