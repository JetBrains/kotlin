// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +AllowContractsOnPropertyAccessors
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts

import kotlin.contracts.*

val String?.foo: String?
    get() {
        contract { (this@foo == null) implies returnsNotNull() }
        return if (this == null) "" else null
    }

fun usage1() {
    "".foo<!UNSAFE_CALL!>.<!>length
}

fun usage2() {
    val x: String? = null
    x.foo<!UNSAFE_CALL!>.<!>length
}

val Any.bar: String?
    get() {
        contract { (this@bar is String) implies returnsNotNull() }
        return if (this is String) "" else null
    }

fun usage3() {
    val s: Any = ""
    s.bar<!UNSAFE_CALL!>.<!>length
}

fun usage4() {
    val n: Any = 1
    n.bar<!UNSAFE_CALL!>.<!>length
}

/* GENERATED_FIR_TAGS: contractImpliesReturnEffect, contracts, equalityExpression, functionDeclaration, getter,
ifExpression, integerLiteral, isExpression, lambdaLiteral, localProperty, nullableType, propertyDeclaration,
propertyWithExtensionReceiver, smartcast, stringLiteral, thisExpression */
