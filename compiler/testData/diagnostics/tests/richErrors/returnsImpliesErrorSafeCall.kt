// RUN_PIPELINE_TILL: CODEGEN
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class A {
    fun returnsTrue(x: Any?): Boolean {
        contract { returns(true) implies (x is String) }
        return x is String
    }

    fun returnsFalse(x: Any?): Boolean {
        contract { returns(false) implies (x is String) }
        return x !is String
    }

    fun returnsNotNull(x: Any?): Any? {
        contract { returnsNotNull() implies (x is String) }
        return when (x) {
            is String -> ""
            else -> null
        }
    }

    fun returnsNull(x: Any?): Any? {
        contract { returns(null) implies (x is String) }
        return when (x) {
            is String -> null
            else -> ""
        }
    }
}

error class Foo

fun test_returnsTrue(a: A | Foo, s: Any?, b: Boolean) {
    if (a|.returnsTrue(s) == true) {
        s.length
    }
    if (a|.returnsTrue(s) == true && b) {
        s.length
    }
}

fun test_returnsFalse(a: A | Foo, s: Any?, b: Boolean) {
    if (a|.returnsFalse(s) == false) {
        s.length
    }
    if (a|.returnsFalse(s) == false && b) {
        s.length
    }
}

fun test_returnsNotNull(a: A | Foo, s: Any?, b: Boolean) {
    if (a|.returnsNotNull(s) != null) {
        s.length
    }
    if (a|.returnsNotNull(s) != null && b) {
        s.length
    }
}

fun test_returnsNull(a: A | Foo, s: Any?, b: Boolean) {
    if (a|.returnsNull(s) == null) {
        s.length
    }
    if (a|.returnsNull(s) == null && b) {
        s.length
    }
}

/* GENERATED_FIR_TAGS: andExpression, classDeclaration, contractConditionalEffect, contracts, equalityExpression,
functionDeclaration, ifExpression, isExpression, lambdaLiteral, nullableType, safeCall, smartcast, stringLiteral,
whenExpression, whenWithSubject */
