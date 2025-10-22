// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +AllowContractsOnPropertyAccessors
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts

// FILE: JMember.java

public class JMember{
    private Boolean flag;
    public JMember(Boolean flag) { this.flag = flag; }
    public Boolean getFlag() { return flag; }
}

// FILE: javaInteropMemberTest.kt

import kotlin.contracts.*

class JHost {
    val JMember?.isNullJM: Boolean
        get() {
            contract { returns(true) implies (this@isNullJM == null) }
            return this == null
        }

    fun caseNullMember(j: JMember?) {
        if (j.isNullJM) {
            return
        } else {
            j<!UNSAFE_CALL!>.<!>getFlag().toString()
        }
    }

    val JMember?.isNotNullJM: Boolean
        get() {
            contract { returns(true) implies (this@isNotNullJM != null) }
            return this == null
        }

    fun caseNotNullMember(j: JMember?) {
        if (j.isNotNullJM) {
            j.getFlag().toString()
        } else {
            j<!UNSAFE_CALL!>.<!>getFlag().toString()
        }
    }

    val JMember?.isFlagTrueM: Boolean
        get() {
            contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(true) implies (this@isFlagTrueM?.getFlag() == true)<!> }
            return this?.getFlag() == true
        }

    fun caseTrueMember(j: JMember?) {
        if (j.isFlagTrueM) {
            j<!UNSAFE_CALL!>.<!>getFlag()
        }
    }
}


fun isNullJTop(j: JMember?): Boolean {
    contract { returns(true) implies (j == null) }
    return j == null
}

fun caseNullTop(j: JMember?) {
    if (isNullJTop(j)) {
        return
    } else {
        j<!UNSAFE_CALL!>.<!>getFlag().toString()
    }
}

fun isNotNullJTop(j: JMember?): Boolean {
    contract { returns(true) implies (j != null) }
    return j == null
}

fun caseNotNullTop(j: JMember?) {
    if (isNotNullJTop(j)) {
        j.getFlag().toString()
    } else {
        j<!UNSAFE_CALL!>.<!>getFlag().toString()
    }
}

fun isFlagTrueTop(j: JMember?): Boolean {
    contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(true) implies (j?.getFlag() == true)<!> }
    return j?.getFlag() == true
}

fun caseTrueTop(j: JMember?) {
    if (isFlagTrueTop(j)) {
        j<!UNSAFE_CALL!>.<!>getFlag()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, contractConditionalEffect, contracts, equalityExpression, flexibleType,
functionDeclaration, getter, ifExpression, javaFunction, javaType, lambdaLiteral, nullableType, propertyDeclaration,
propertyWithExtensionReceiver, safeCall, smartcast, thisExpression */
