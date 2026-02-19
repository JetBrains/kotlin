// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +AllowContractsOnPropertyAccessors
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts

import kotlin.contracts.*

var String?.ensureNull: Unit
    get() = this.ensureNull
    set(v) {
        contract { returns() implies (this@ensureNull == null) }
        this.ensureNull = v
    }

fun testSetterTruth(x: String?) {
    x.ensureNull = Unit
    x<!UNSAFE_CALL!>.<!>length
}

var String?.ensureNonNull: Unit
    get() = this.ensureNonNull
    set(v) {
        contract { returns() implies (this@ensureNonNull != null) }
        this.ensureNonNull = v
    }

fun testSetterLiar(x: String?) {
    x.ensureNonNull = Unit
    x.length
}


class Host {
    var String?.ensureNullM: Unit
        get() = this.ensureNullM
        set(v) {
            contract { returns() implies (this@ensureNullM == null) }
            this.ensureNullM = v
        }

    fun testSetterTruthMember(x: String?) {
        x.ensureNullM = Unit
        x<!UNSAFE_CALL!>.<!>length
    }

    var String?.ensureNonNullM: Unit
        get() = this.ensureNonNullM
        set(v) {
            contract { returns() implies (this@ensureNonNullM != null) }
            this.ensureNonNullM = v
        }

    fun testSetterLiarMember(x: String?) {
        x.ensureNonNullM = Unit
        x.length
    }
}


fun setEnsureNullTop(recv: String?, v: Unit) {
    contract { returns() implies (recv == null) }
}

fun testSetterTruthTop(x: String?) {
    setEnsureNullTop(x, Unit)
    x<!UNSAFE_CALL!>.<!>length
}

fun setEnsureNonNullTop(recv: String?, v: Unit) {
    contract { returns() implies (recv != null) }
}

fun testSetterLiarTop(x: String?) {
    setEnsureNonNullTop(x, Unit)
    x.length
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, contractConditionalEffect, contracts, equalityExpression,
functionDeclaration, getter, lambdaLiteral, nullableType, propertyDeclaration, propertyWithExtensionReceiver, setter,
smartcast, thisExpression */
