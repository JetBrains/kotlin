// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +AllowContractsOnPropertyAccessors
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts

package samInterface

import kotlin.contracts.*

fun interface SamInterface {
    fun Int?.accept(): String
}

val Int?.a: String
    get() {
        contract {
            returns() implies (this@a != null)
        }
        return "OK"
    }

val c = SamInterface ( Int?::a )

fun box(x: Int?) {
    with(c) {
        x.accept()
        x<!UNSAFE_CALL!>.<!>inc()
    }
}

class Host {
    val Int?.aM: String
        get() {
            contract { returns() implies (this@aM != null) }
            return "OK"
        }
}

fun boxMember(h: Host, x: Int?) {
    with(h) {
        val c = SamInterface { this.aM }

        with(c) {
            x.accept()
            x<!UNSAFE_CALL!>.<!>inc()
        }
    }
}

fun aTop(x: Int?): String {
    contract { returns() implies (x != null) }
    return "OK"
}

val cTop = SamInterface(::aTop)

fun boxTop(x: Int?) {
    with(cTop) {
        x.accept()
        x<!UNSAFE_CALL!>.<!>inc()
    }
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, contractConditionalEffect, contracts, equalityExpression,
funInterface, funWithExtensionReceiver, functionDeclaration, getter, interfaceDeclaration, lambdaLiteral, localProperty,
nullableType, propertyDeclaration, propertyWithExtensionReceiver, stringLiteral, thisExpression */
