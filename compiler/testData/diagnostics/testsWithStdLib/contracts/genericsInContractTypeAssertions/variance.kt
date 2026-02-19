// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +AllowCheckForErasedTypesInContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

@Suppress("REDUNDANT_PROJECTION")
fun List<*>.isCovariantString(): Boolean {
    contract { returns(true) implies (this@isCovariantString is List<out String>) }
    return all { it is String }
}

fun usageOut(xs: List<Any?>) {
    if (xs.isCovariantString()) {
        xs.first().length
    }
}

@Suppress("REDUNDANT_PROJECTION")
typealias StringOutList = List<out String>

fun usageOutTA(xs: Any): Boolean {
    return xs is <!CANNOT_CHECK_FOR_ERASED!>StringOutList<!>
}

fun MutableList<*>.acceptsString(): Boolean {
    contract { returns(true) implies (this@acceptsString is MutableList<in String>) }
    return true
}

fun usageIn(xs: MutableList<Any?>) {
    if (xs.acceptsString()) {
        xs.add("")
    }
}

typealias StringInList = MutableList<in String>

fun usageInTA(xs: Any): Boolean {
    return xs is <!CANNOT_CHECK_FOR_ERASED!>StringInList<!>
}

/* GENERATED_FIR_TAGS: contractConditionalEffect, contracts, funWithExtensionReceiver, functionDeclaration, ifExpression,
inProjection, isExpression, lambdaLiteral, nullableType, outProjection, smartcast, starProjection, stringLiteral,
thisExpression, typeAliasDeclaration */
