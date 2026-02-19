// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +AllowContractsOnPropertyAccessors
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts

import kotlin.contracts.*

val Any?.combinedOr: Boolean
    get() {
        contract {
            returns(true) implies ((this@combinedOr == null) || (this@combinedOr is String))
        }
        return (this == null) || (this is String)
    }

fun testCombinedOr(x: Any?) {
    if (x.combinedOr) {
        when (x) {
            null -> "null"
            is String -> x.length
        }
    } else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

val String?.isNotBlankOrNull: Boolean
    get() {
        contract {
            <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(true) implies (this@isNotBlankOrNull != null && this@isNotBlankOrNull.isNotBlank())<!>
        }
        return this != null && isNotBlank()
    }

fun testIsNotBlankOrNull(s: String?) {
    if (s.isNotBlankOrNull) {
        s<!UNSAFE_CALL!>.<!>length
    }
}

var String?.nonEmptyOrNull: String
    get() {
        contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>returnsNotNull() implies (this@nonEmptyOrNull != null && this@nonEmptyOrNull.isNotEmpty())<!> }
        return this ?: ""
    }
    set(value) {
        contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>returns() implies (value.isNotEmpty())<!> }
    }

fun testNonEmptyOrNull(s: String?) {
    val ss = s.nonEmptyOrNull
    s<!UNSAFE_CALL!>.<!>length
    ss.length
    s.nonEmptyOrNull = ""
}

@Suppress("USELESS_IS_CHECK")
val Any?.combinedAndNot: Boolean
    get() {
        contract {
            returns(true) implies (this@combinedAndNot is String && this@combinedAndNot !is CharSequence)
            returns(false) implies !(this@combinedAndNot is String && this@combinedAndNot !is CharSequence)
        }
        return (this is String && this !is CharSequence)
    }

fun testCombinedAndNot(x: Any?) {
    if (x.combinedAndNot) {
        x.length
    } else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

/* GENERATED_FIR_TAGS: andExpression, assignment, comparisonExpression, contractConditionalEffect, contracts,
disjunctionExpression, elvisExpression, equalityExpression, functionDeclaration, getter, ifExpression, integerLiteral,
isExpression, lambdaLiteral, localProperty, nullableType, propertyDeclaration, propertyWithExtensionReceiver, setter,
smartcast, stringLiteral, thisExpression, whenExpression, whenWithSubject */
