// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +HoldsInContracts, +ContextParameters
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts
// ISSUE: KT-79324, KT-79325
import kotlin.contracts.contract

context(condition: Boolean)
inline val <R> (() -> R).conditionInContext: R?
    get() {
        contract { condition holdsIn this@conditionInContext }
        return null
    }

context(condition: Boolean, block: () -> R)
inline val <R> conditionAndBlockInContext: R?
    get() {
        contract { condition holdsIn block }            //KT-79324 should be ERROR_IN_CONTRACT_DESCRIPTION
        return if (condition) {
            block()
        } else null
    }

context(condition: T)
inline val <T> (() -> T).withTypeParameter: Unit
    get() {
        contract { (condition as Boolean) holdsIn this@withTypeParameter }
    }

fun test(x: String?) {
    with(x is String) {
        {
            x<!UNSAFE_CALL!>.<!>length          //KT-79325 should be OK
        }.conditionInContext
    }
}


fun test2(x: String?) {
    with(x is String) {
        with({
            x<!UNSAFE_CALL!>.<!>length
        }) {
            conditionAndBlockInContext
        }
    }
}

fun test3(x: String?) {
    with(x is String) {
        {
            x<!UNSAFE_CALL!>.<!>length      //KT-79325 should be OK
            1
        }.withTypeParameter
    }
}

/* GENERATED_FIR_TAGS: asExpression, functionDeclaration, functionalType, getter, ifExpression, integerLiteral,
intersectionType, isExpression, lambdaLiteral, nullableType, propertyDeclaration, propertyDeclarationWithContext,
propertyWithExtensionReceiver, thisExpression, typeParameter */
