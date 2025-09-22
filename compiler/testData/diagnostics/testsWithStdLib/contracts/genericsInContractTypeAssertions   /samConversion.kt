// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +AllowCheckForErasedTypesInContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

fun interface Predicate { fun test(v: Any?): Boolean }

fun <T: Any> isType(v: Any?): Boolean {
    contract { returns(true) implies (v is T) }
    return true
}

val stringPred: Predicate = Predicate { v -> isType<String>(v) }

fun usage(x: Any?) {
    if (stringPred.test(x)) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

/* GENERATED_FIR_TAGS: contractConditionalEffect, contracts, funInterface, functionDeclaration, ifExpression,
interfaceDeclaration, isExpression, lambdaLiteral, nullableType, propertyDeclaration, typeConstraint, typeParameter */
