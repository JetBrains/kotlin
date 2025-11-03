// LANGUAGE: +HoldsInContracts
@file:OptIn(ExperimentalContracts::class, ExperimentalExtendedContracts::class)
// ISSUE: KT-79329
import kotlin.contracts.*

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

inline fun testConditionInDescription6(condition: Boolean, condition2: Boolean, block: () -> Unit) {
    contract { (condition and condition2) holdsIn block }       //KT-79329 should be ERROR_IN_CONTRACT_DESCRIPTION
    block()
}

inline fun testConditionInDescription10(condition: Boolean, block: () -> Unit) {
    contract { foo@(condition holdsIn block) }
    block()
}
