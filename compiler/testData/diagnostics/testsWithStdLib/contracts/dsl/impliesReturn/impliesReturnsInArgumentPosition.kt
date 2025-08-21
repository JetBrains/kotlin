// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ConditionImpliesReturnsContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts
// ISSUE: KT-79277, KT-79526
import kotlin.contracts.*

fun notNullIfNotNull(encoded: String?): Boolean? {
    contract {
        (encoded != null) implies (returnsNotNull())
    }
    return encoded != null
}

fun String?.notNullIfNull(): Boolean? {
    contract {
        (this@notNullIfNull == null) implies returnsNotNull()
    }
    return this !== null
}

val String?.notNullIfNull: Boolean?
    get() {
        contract {
            (this@notNullIfNull == null) implies returnsNotNull()
        }
        return this !== null
    }

fun nestedUsage(x: String) {
    val a = null
    acceptBoolean(notNullIfNotNull(x))
    acceptBoolean(<!ARGUMENT_TYPE_MISMATCH!>notNullIfNotNull(null)<!>)
    acceptBoolean(<!ARGUMENT_TYPE_MISMATCH!>notNullIfNotNull(a)<!>)

    acceptBoolean(<!ARGUMENT_TYPE_MISMATCH!>x.notNullIfNull()<!>)
    acceptBoolean(null.notNullIfNull())
    acceptBoolean(a.notNullIfNull())

    acceptBoolean(<!ARGUMENT_TYPE_MISMATCH!>x.notNullIfNull<!>)
    acceptBoolean(null.notNullIfNull)
    acceptBoolean(a.notNullIfNull)

    val b = null.notNullIfNull
    acceptBoolean(b)

    val c = null.notNullIfNull()
    acceptBoolean(c)

    val d = a.notNullIfNull
    acceptBoolean(<!ARGUMENT_TYPE_MISMATCH!>d<!>)       //KT-79526 should be OK

    val e = a.notNullIfNull()
    acceptBoolean(e)
}

fun acceptBoolean(x: Boolean) {}

/* GENERATED_FIR_TAGS: contractImpliesReturnEffect, contracts, equalityExpression, funWithExtensionReceiver,
functionDeclaration, getter, lambdaLiteral, localProperty, nullableType, propertyDeclaration,
propertyWithExtensionReceiver, smartcast, thisExpression */
