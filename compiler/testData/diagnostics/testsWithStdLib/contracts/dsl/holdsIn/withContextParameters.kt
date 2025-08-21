// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +HoldsInContracts, +ContextParameters
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts
// ISSUE: KT-79324, KT-79325
import kotlin.contracts.contract

context(a: Boolean)
inline fun conditionInContext(block: () -> Int) {
    contract { a holdsIn block }
    block()
}

context(a: () -> Int)
<!NOTHING_TO_INLINE!>inline<!> fun lambdaInContext(condition: Boolean): Unit {
    contract { condition holdsIn a }            //KT-79324 should be ERROR_IN_CONTRACT_DESCRIPTION
    a()
}

fun usage(s: String?) {
    with(s is String) {
        conditionInContext {
            s<!UNSAFE_CALL!>.<!>length          //KT-79325 should be OK
        }
    }

    with ({ s<!UNSAFE_CALL!>.<!>length }) {
        lambdaInContext(s is String)
    }
}

/* GENERATED_FIR_TAGS: contractHoldsInEffect, contracts, functionDeclaration, functionDeclarationWithContext,
functionalType, inline, isExpression, lambdaLiteral, nullableType */
