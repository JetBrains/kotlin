// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +HoldsInContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts
// ISSUE: KT-79329
import kotlin.contracts.contract

inline fun testConditionInDescription(condition: Boolean?, block: () -> Unit) {
    contract { (condition == null) holdsIn block }              //KT-79329 should be ERROR_IN_CONTRACT_DESCRIPTION
    block()
}

inline fun testConditionInDescription2(condition: Boolean?, block: () -> Unit) {
    contract { (condition is Boolean) holdsIn block }           //KT-79329 should be ERROR_IN_CONTRACT_DESCRIPTION
    block()
}

inline fun testConditionInDescription3(condition: Boolean, condition2: Boolean, block: () -> Unit) {
    contract { (condition && condition2) holdsIn block }        //KT-79329 should be ERROR_IN_CONTRACT_DESCRIPTION
    block()
}

inline fun testConditionInDescription4(condition: Boolean, block: () -> Unit) {
    contract { !condition holdsIn block }
    block()
}

inline fun testConditionInDescription5(a: Boolean?, block: () -> Unit) {
    contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>a!! holdsIn block<!> }
    block()
}

inline fun testConditionInDescription6(condition: Boolean, condition2: Boolean, block: () -> Unit) {
    contract { (condition and condition2) holdsIn block }       //KT-79329 should be ERROR_IN_CONTRACT_DESCRIPTION
    block()
}

inline fun testConditionInDescription7(a: Int, block: () -> Unit) {
    contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>(a in 1..10) holdsIn block<!> }
    block()
}

inline fun testConditionInDescription8(block: () -> Unit) {
    contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>(Boolean::class.<!NO_REFLECTION_IN_CLASS_PATH!>isOpen<!>) holdsIn block<!> }
    block()
}

inline fun testConditionInDescription9(condition: Boolean?, block: () -> Unit) {
    contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>(condition ?: true) holdsIn block<!> }
    block()
}

inline fun testConditionInDescription10(condition: Boolean, block: () -> Unit) {
    contract { foo@(condition holdsIn block) }
    block()
}

inline fun testConditionInDescription11(condition: ()-> Boolean, block: () -> Unit) {
    contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>condition() holdsIn block<!> }
    block()
}

/* GENERATED_FIR_TAGS: andExpression, checkNotNullCall, classReference, contractHoldsInEffect, contracts,
elvisExpression, equalityExpression, functionDeclaration, functionalType, inline, integerLiteral, isExpression,
lambdaLiteral, nullableType, rangeExpression */
