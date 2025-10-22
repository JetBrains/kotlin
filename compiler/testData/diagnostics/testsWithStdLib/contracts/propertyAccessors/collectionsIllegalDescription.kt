// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +AllowContractsOnPropertyAccessors
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts

import kotlin.contracts.*

val List<String>.maybeFirst: String?
    get() {
        contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(null) implies (this@maybeFirst.isEmpty())<!> }
        return firstOrNull()
    }

fun usage1(l: List<String>) {
    if (l.maybeFirst == null) {
        l.size
    }
}

val <T> List<T>?.firstOrEmpty: T
    get() {
        contract {
            <!ERROR_IN_CONTRACT_DESCRIPTION!>returns() implies (this@firstOrEmpty != null && this@firstOrEmpty.isNotEmpty())<!>
        }
        return this!!.first()
    }

fun usage2(l: List<String>?) {
    if (l.firstOrEmpty.isNotEmpty()) {
        l<!UNSAFE_CALL!>.<!>size
    }
}

val <T> List<T>.firstNonNull: T
    get() {
        contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>returns() implies (this@firstNonNull.isNotEmpty())<!> }
        return first()
    }

fun usage3(l: List<Int>) {
    l.firstNonNull
}

/* GENERATED_FIR_TAGS: andExpression, checkNotNullCall, contractConditionalEffect, contracts, equalityExpression,
functionDeclaration, getter, ifExpression, lambdaLiteral, nullableType, propertyDeclaration,
propertyWithExtensionReceiver, smartcast, thisExpression, typeParameter */
