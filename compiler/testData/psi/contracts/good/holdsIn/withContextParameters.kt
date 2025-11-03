// LANGUAGE: +HoldsInContracts, +ContextParameters
@file:OptIn(ExperimentalContracts::class, ExperimentalExtendedContracts::class)
// ISSUE: KT-79324, KT-79325
import kotlin.contracts.*

context(a: Boolean)
inline fun conditionInContext(block: () -> Int) {
    contract { a holdsIn block }
    block()
}

context(a: () -> Int)
inline fun lambdaInContext(condition: Boolean): Unit {
    contract { condition holdsIn a }            //KT-79324 should be ERROR_IN_CONTRACT_DESCRIPTION
    a()
}
