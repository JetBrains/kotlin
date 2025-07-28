// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +HoldsInContracts, +AllowContractsOnSomeOperators
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts
import kotlin.contracts.contract

inline operator fun Boolean.invoke(block:()-> Unit) {
    contract { this@invoke holdsIn block }
}

fun test(x: String?){
    (x is String) {
        x.length
    }
}

/* GENERATED_FIR_TAGS: contractHoldsInEffect, contracts, funWithExtensionReceiver, functionDeclaration, functionalType,
inline, isExpression, lambdaLiteral, nullableType, operator, smartcast, thisExpression */
