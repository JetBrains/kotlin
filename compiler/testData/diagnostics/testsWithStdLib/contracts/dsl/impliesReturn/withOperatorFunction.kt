// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ConditionImpliesReturnsContracts, +AllowContractsOnSomeOperators
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts

import kotlin.contracts.*

operator fun Int?.invoke(): String? {
    contract {
        (this@invoke != null) implies (returnsNotNull())
    }
    return ""
}

fun usage(x: Int) {
    x().length
}

/* GENERATED_FIR_TAGS: contractImpliesReturnEffect, contracts, equalityExpression, funWithExtensionReceiver,
functionDeclaration, lambdaLiteral, nullableType, operator, smartcast, stringLiteral, thisExpression */
