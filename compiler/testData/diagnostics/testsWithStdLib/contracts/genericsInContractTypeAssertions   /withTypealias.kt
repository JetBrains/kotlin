// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +AllowCheckForErasedTypesInContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts

import kotlin.contracts.*

typealias Str = String

typealias StringList = List<String>

fun isStr(v: Any?): Boolean {
    contract { returns(true) implies (v is Str) }
    return v is Str
}

fun isStringList(list: Any?): Boolean {
    contract { returns(true) implies (list is StringList) }
    return list is <!CANNOT_CHECK_FOR_ERASED!>StringList<!>
}

fun isStringListStar(list: Any?): Boolean {
    contract { returns(true) implies (list is StringList) }
    return list is List<*>
}

fun usageIsStr(x: Any) {
    if (isStr(x)) x.length
}

fun usageIsStringList(x: Any) {
    if (isStringList(x)) x[0].length
}

fun usageIsStringListStar(x: Any) {
    if (isStringListStar(x)) x.<!UNRESOLVED_REFERENCE!>length<!>
}

/* GENERATED_FIR_TAGS: contractConditionalEffect, contracts, functionDeclaration, ifExpression, integerLiteral,
isExpression, lambdaLiteral, nullableType, smartcast, starProjection, typeAliasDeclaration */
