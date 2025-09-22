// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +AllowContractsOnPropertyAccessors
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts

// FILE: J.java

public class J {
    private Boolean flag;
    public J(Boolean flag) { this.flag = flag; }
    public Boolean getFlag() { return flag; }
}

// FILE: javaInteropTest.kt

import kotlin.contracts.*

val J?.isNullJ: Boolean
    get() {
        contract { returns(true) implies (this@isNullJ == null) }
        return this == null
    }

fun caseNull(j: J?) {
    if (j.isNullJ) {
        return
    } else {
        j<!UNSAFE_CALL!>.<!>getFlag().toString()
    }
}

val J?.isNotNullJ: Boolean
    get() {
        contract { returns(true) implies (this@isNotNullJ != null) }
        return this == null
    }

fun caseNotNull(j: J?) {
    if (j.isNotNullJ) {
        j.getFlag().toString()
    } else {
        j<!UNSAFE_CALL!>.<!>getFlag().toString()
    }
}

val J?.isFlagTrue: Boolean
    get() {
        contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(true) implies (this@isFlagTrue?.getFlag() == true)<!> }
        return this?.getFlag() == true
    }

fun caseTrue(j: J?) {
    if (j.isFlagTrue) {
        j<!UNSAFE_CALL!>.<!>getFlag()
    }
}

/* GENERATED_FIR_TAGS: contractConditionalEffect, contracts, equalityExpression, flexibleType, functionDeclaration,
getter, ifExpression, javaFunction, javaType, lambdaLiteral, nullableType, propertyDeclaration,
propertyWithExtensionReceiver, safeCall, smartcast, thisExpression */
