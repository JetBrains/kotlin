// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +AllowContractsOnPropertyAccessors
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts

import kotlin.contracts.*

class Host {
    inline val <T> T?.isNotNullM: Boolean
        get() {
            contract { returns(true) implies (this@isNotNullM != null) }
            return this != null
        }

    fun processStringMember(str: String?) {
        if (str.isNotNullM) {
            str.length
        } else {
            str<!UNSAFE_CALL!>.<!>length
        }
    }

    val String?.notNullInlineGetterM: Boolean
        inline get() {
            contract { returns(true) implies (this@notNullInlineGetterM != null) }
            return this != null
        }

    fun testInlineGetterMember(x: String?) {
        if (x.notNullInlineGetterM) {
            x.length
        } else {
            x<!UNSAFE_CALL!>.<!>length
        }
    }

    var Int?.aM: String
        get() = ""
        inline set(value) {
            contract {
                returns() implies (this@aM != null)
            }
        }
}

@Suppress("NOTHING_TO_INLINE")
inline fun <T> isNotNull_top(x: T?): Boolean {
    contract { returns(true) implies (x != null) }
    return x != null
}

fun processString_top(str: String?) {
    if (isNotNull_top(str)) {
        str.length
    } else {
        str<!UNSAFE_CALL!>.<!>length
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun notNullInlineGetter_top(x: String?): Boolean {
    contract { returns(true) implies (x != null) }
    return x != null
}

fun testInlineGetter_top(x: String?) {
    if (notNullInlineGetter_top(x)) {
        x.length
    } else {
        x<!UNSAFE_CALL!>.<!>length
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun setATop(recv: Int?, value: String) {
    contract { returns() implies (recv != null) }
}

/* GENERATED_FIR_TAGS: classDeclaration, contractConditionalEffect, contracts, equalityExpression, functionDeclaration,
getter, ifExpression, inline, lambdaLiteral, nullableType, propertyDeclaration, propertyWithExtensionReceiver, setter,
smartcast, stringLiteral, thisExpression, typeParameter */
