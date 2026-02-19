// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ConditionImpliesReturnsContracts, +AllowContractsOnPropertyAccessors
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts
// ISSUE: KT-79218, KT-79220, KT-79526
import kotlin.contracts.contract

val String?.foo: String?
    get() {
        contract { (this@foo == null) implies returnsNotNull() }
        return if (this == null) "" else null
    }

fun String?.foo(): String? {
    contract { (this@foo == null) implies returnsNotNull() }
    return if (this == null) "" else null
}

fun test() {
    null.foo.length
    null.foo().length

    val x = null
    x.foo.length
    x.foo().length

    val a = null.foo
    val b = null.foo()
    a.length
    b.length

    val c = x.foo
    val d = x.foo()

    c<!UNSAFE_CALL!>.<!>length      //KT-79526 should be OK
    d.length
}

/* GENERATED_FIR_TAGS: contractImpliesReturnEffect, contracts, equalityExpression, funWithExtensionReceiver,
functionDeclaration, getter, ifExpression, lambdaLiteral, localProperty, nullableType, propertyDeclaration,
propertyWithExtensionReceiver, smartcast, stringLiteral, thisExpression */
