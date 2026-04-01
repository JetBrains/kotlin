// LANGUAGE: +HoldsInContracts
@file:OptIn(ExperimentalContracts::class, ExperimentalExtendedContracts::class)
// ISSUE: KT-79025
import kotlin.contracts.*

inline fun Boolean.testConditionInExtension(block: () -> Unit) {
    contract { this@testConditionInExtension holdsIn block }
    block()
}

inline fun (() -> Int).testLambdaInExtension(condition: Boolean) {
    contract { condition holdsIn this@testLambdaInExtension }       //KT-79025 should be ERROR_IN_CONTRACT_DESCRIPTION
    this()
}
