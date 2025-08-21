// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ConditionImpliesReturnsContracts, +AllowContractsOnPropertyAccessors
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts
// ISSUE: KT-79526
import kotlin.contracts.*

val String?.foo: String?
    get() {
        contract { (this@foo == null) implies returnsNotNull() }
        return if (this == null) "" else null
    }

fun String?.foo(): String? {
    contract { (this@foo == null) implies returnsNotNull() }
    return if (this == null) "" else null
}

class CheckOnMemberWithGetter {
    val x: Nothing?
        get() = null

    fun usage() {
        x.foo.length
        x.foo().length

        val a = x.foo
        val b = x.foo()
        a<!UNSAFE_CALL!>.<!>length      //KT-79526 should be OK
        b.length
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, contractImpliesReturnEffect, contracts, equalityExpression,
funWithExtensionReceiver, functionDeclaration, getter, ifExpression, lambdaLiteral, localProperty, nullableType,
propertyDeclaration, propertyWithExtensionReceiver, smartcast, stringLiteral, thisExpression */
