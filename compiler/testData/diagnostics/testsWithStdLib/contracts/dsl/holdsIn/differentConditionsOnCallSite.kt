// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +HoldsInContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts
// ISSUE: KT-79329
import kotlin.contracts.contract

inline fun testCondition(condition: Boolean, block: () -> Unit) {
    contract { condition holdsIn block }
    block()
}

inline fun testStringNotNull(condition: String?, block: () -> Unit) {
    contract { (condition != null) holdsIn block }                      //KT-79329 complex condition should be forbidden
    block()
}

fun usageConditions(a: String?, b: String?, c: Any?, d: Any, e: Any) {
    testCondition(a != null) {
        a.length
    }

    testCondition(a == null) {
        <!SENSELESS_COMPARISON!>a == null<!>
    }

    testCondition(a == "") {
        a.length
    }

    testCondition(a is String) {
        a.length
    }

    testCondition(a !is Nothing?) {
        a.length
    }

    testCondition(a is String && b is String) {
        a.length
        b.length
    }

    testCondition(a is String || b is String) {
        a<!UNSAFE_CALL!>.<!>length
        b<!UNSAFE_CALL!>.<!>length
    }

    testCondition(!(a !is String)) {
        a.length
    }

    testCondition(c is String? && c != null || d is Int) {
        c.<!UNRESOLVED_REFERENCE!>length<!>
        d.<!UNRESOLVED_REFERENCE!>inc<!>()
    }

    testCondition(a is String || return) {
        a.length
    }
    a.length

    testCondition(b !is String && throw Exception()) {
        b.length
    }
    b.length

    val x = c is String
    testCondition(x) {
        c.length
    }

    testStringNotNull(e as String) {
        e.length
    }
}

/* GENERATED_FIR_TAGS: andExpression, contractHoldsInEffect, contracts, disjunctionExpression, equalityExpression,
functionDeclaration, functionalType, inline, isExpression, lambdaLiteral, localProperty, nullableType,
propertyDeclaration, smartcast, stringLiteral */
