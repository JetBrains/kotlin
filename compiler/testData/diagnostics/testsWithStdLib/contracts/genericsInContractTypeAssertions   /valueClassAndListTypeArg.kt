// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +AllowCheckForErasedTypesInContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts

import kotlin.contracts.*

@JvmInline
value class Name(val value: String)

fun isName(v: Any?): Boolean {
    contract { returns(true) implies (v is Name) }
    return v is Name
}

fun isNameList(v: Any?): Boolean {
    contract { returns(true) implies (v is List<Name>) }
    return v is <!CANNOT_CHECK_FOR_ERASED!>List<Name><!>
}

fun isNameListStar(v: Any?): Boolean {
    contract { returns(true) implies (v is List<Name>) }
    return v is List<*>
}

fun usageIsName(x: Any?) {
    if (isName(x)) {
        x.value
    }
}

fun usageIsNameList(x: Any?) {
    if (isNameList(x)) {
        x.<!UNRESOLVED_REFERENCE!>value<!>
    }
}

fun usageIsNameListStar(x: Any?) {
    if (isNameListStar(x)) {
        x.<!UNRESOLVED_REFERENCE!>value<!>
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, contractConditionalEffect, contracts, functionDeclaration, ifExpression,
isExpression, lambdaLiteral, nullableType, primaryConstructor, propertyDeclaration, smartcast, starProjection, value */
