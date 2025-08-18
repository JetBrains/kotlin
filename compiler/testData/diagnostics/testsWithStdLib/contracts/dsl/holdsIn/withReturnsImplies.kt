// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +HoldsInContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts
import kotlin.contracts.contract

fun <T> materialize(): T = null!!

inline fun <R> runIf(condition: Boolean, block: () -> R): R {
    contract { condition holdsIn block }
    return null!!
}

fun returnsImpliesBeforeHoldsIn() {
    var a :Any? = materialize()
    require(a is Int)
    runIf(<!IMPOSSIBLE_IS_CHECK_ERROR!>a is String<!>) {
        a.length
    }
}

fun returnsImplesAfterHoldsIn() {
    var a: Any? = materialize()
    runIf(a is String) {
        require(<!IMPOSSIBLE_IS_CHECK_ERROR!>a is Int<!>)
        a.length
    }
}

/* GENERATED_FIR_TAGS: checkNotNullCall, contractHoldsInEffect, contracts, functionDeclaration, functionalType, inline,
intersectionType, isExpression, lambdaLiteral, localProperty, nullableType, propertyDeclaration, smartcast,
typeParameter */
