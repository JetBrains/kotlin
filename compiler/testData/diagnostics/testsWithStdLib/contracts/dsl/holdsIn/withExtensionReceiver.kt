// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +HoldsInContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts
// ISSUE: KT-79025
import kotlin.contracts.contract

inline fun Boolean.testConditionInExtension(block: () -> Unit) {
    contract { this@testConditionInExtension holdsIn block }
    block()
}

<!NOTHING_TO_INLINE!>inline<!> fun (() -> Int).testLambdaInExtension(condition: Boolean) {
    contract { condition holdsIn this@testLambdaInExtension }       //KT-79025 should be ERROR_IN_CONTRACT_DESCRIPTION
    this()
}

fun test(a: String?) {
    (a is String).testConditionInExtension {
        a.length
    };

    { a<!UNSAFE_CALL!>.<!>length }.testLambdaInExtension(a is String)
}

/* GENERATED_FIR_TAGS: contractHoldsInEffect, contracts, funWithExtensionReceiver, functionDeclaration, functionalType,
inline, isExpression, lambdaLiteral, nullableType, smartcast, thisExpression */
